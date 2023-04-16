package manager;

import java.util.ArrayList;
import java.util.List;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IInterceptedProxyMessage;
import config.ConfigEntry;
import config.GUI;

public class DismissedTargetsManager {

	private static final String Forward = "Forward";
	private static final String Drop = "Drop";

	public static String getHost(IHttpRequestResponse message) {//如果存在host参数，会被用于序列化，注意
		String host = message.getHttpService().getHost();
		return host;
	}

	public static String getUrl(IHttpRequestResponse message) {//如果存在host参数，会被用于序列化，注意
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		String url = new HelperPlus(helpers).getFullURL(message).toString();
		if (url.contains("?")){
			url = url.substring(0,url.indexOf("?"));
		}
		return url;
	}

	/**
	 * 修改规则前后，都应该和GUI同步
	 * @param messages
	 * @param action
	 */
	public static void putRule(IHttpRequestResponse[] messages,String keyword,String action ) {

		if (messages != null) {
			for(IHttpRequestResponse message:messages) {
				String host = getHost(message);
				String url = getUrl(message);

				if (action.equalsIgnoreCase(ConfigEntry.Action_Drop_Request_If_Host_Matches)
						|| action.equalsIgnoreCase(ConfigEntry.Action_Forward_Request_If_Host_Matches)) {
					GUI.tableModel.addNewConfigEntry(new ConfigEntry(host, "",action,true));
				}

				if (action.equalsIgnoreCase(ConfigEntry.Action_Drop_Request_If_URL_Matches)
						|| action.equalsIgnoreCase(ConfigEntry.Action_Forward_Request_If_URL_Matches)) {
					GUI.tableModel.addNewConfigEntry(new ConfigEntry(url, "",action,true));
				}
			}
		}

		if (keyword != null && !keyword.equals("")) {
			if (action.equalsIgnoreCase(ConfigEntry.Action_Drop_Request_If_Keyword_Matches)
					|| action.equalsIgnoreCase(ConfigEntry.Action_Forward_Request_If_Keyword_Matches)) {
				GUI.tableModel.addNewConfigEntry(new ConfigEntry(keyword, "",action,true));
			}
		}
	}

	/**
	 * 
	 * @param messages
	 */
	public static void removeRule(IHttpRequestResponse[] messages) {
		for(IHttpRequestResponse message:messages) {
			while (true) {
				//有可能多个规则都影响某个数据包
				MatchResult res = whichAction(message);
				if (res.getAction() == null || res.getAction().equals("")){
					break;//无规则命中
				}else {
					ConfigEntry Rule =res.getRule();
					if (Rule != null) {
						GUI.tableModel.removeConfigEntry(Rule);
					}
				}
			}
		}
	}

	/**
	 * 获取当前数据包应该执行的action
	 * 规则的匹配按照时间优先顺序进行：
	 * 越是新的规则，优先级越高。因为越是新的设置，越是能表示操作者当前的意愿。
	 * @param message
	 * @return drop forward "" 空字符串表示什么也不做
	 */
	private static MatchResult whichAction(IHttpRequestResponse message) {

		String host = getHost(message);//域名不应该大小写敏感
		String url = getUrl(message);//URL中可能包含大写字母比如getUserInfo，URL应该是大小写敏感的。

		List<ConfigEntry> rules = GetRules();

		for (int index=rules.size()-1;index>=0;index--) {
			ConfigEntry rule = rules.get(index);

			if (rule.getType().equalsIgnoreCase(ConfigEntry.Action_Drop_Request_If_Host_Matches)) {
				if (host.equalsIgnoreCase(rule.getKey())) {
					return new MatchResult(Drop, rule);
				}

				if (rule.getKey().startsWith("*.")) {
					String tmpDomain = rule.getKey().replaceFirst("\\*","");
					if (host.toLowerCase().endsWith(tmpDomain.toLowerCase())){
						return new MatchResult(Drop, rule);
					}
				}
			}
			if (rule.getType().equalsIgnoreCase(ConfigEntry.Action_Drop_Request_If_URL_Matches)) {
				if (url.equalsIgnoreCase(rule.getKey())) {
					return new MatchResult(Drop, rule);
				}
			}
			if (rule.getType().equalsIgnoreCase(ConfigEntry.Action_Drop_Request_If_Keyword_Matches)) {
				if (url.contains(rule.getKey())) {
					return new MatchResult(Drop, rule);
				}
			}
			if (rule.getType().equalsIgnoreCase(ConfigEntry.Action_Forward_Request_If_Host_Matches)) {
				if (host.equalsIgnoreCase(rule.getKey())) {
					return new MatchResult(Forward, rule);
				}

				if (rule.getKey().startsWith("*.")) {
					String tmpDomain = rule.getKey().replaceFirst("\\*","");
					if (host.toLowerCase().endsWith(tmpDomain.toLowerCase())){
						return new MatchResult(Forward, rule);
					}
				}
			}
			if (rule.getType().equalsIgnoreCase(ConfigEntry.Action_Forward_Request_If_URL_Matches)) {
				if (url.equalsIgnoreCase(rule.getKey())) {
					return new MatchResult(Forward, rule);
				}
			}
			if (rule.getType().equalsIgnoreCase(ConfigEntry.Action_Forward_Request_If_Keyword_Matches)) {
				if (url.contains(rule.getKey())) {
					return new MatchResult(Forward, rule);
				}
			}
		}

		return new MatchResult(null,null);
	}

	public static List<ConfigEntry> GetRules() {
		List<ConfigEntry> result = new ArrayList<ConfigEntry>();

		List<ConfigEntry> entries = GUI.tableModel.getConfigEntries();
		for (ConfigEntry entry:entries) {
			if (entry.isDropOrForwardActionType()) {
				if (entry.isEnable()) {
					result.add(entry);
				}
			}
		}
		return result;
	}


	/**
	 * 返回这个数据是否被丢弃或者转发了。以便上层逻辑决定是否需要继续处理
	 * @param message
	 * @return
	 */
	public static boolean checkAndDoAction(boolean messageIsRequest,IInterceptedProxyMessage message) {
		if (messageIsRequest) {
			MatchResult res = whichAction(message.getMessageInfo());
			BurpExtender.getStdout().println(res.toString());
			if(res.getAction()== null) {
				return false;
			}
			if (res.getAction().equalsIgnoreCase(Forward)){
				message.setInterceptAction(IInterceptedProxyMessage.ACTION_DONT_INTERCEPT);
				message.getMessageInfo().setComment("Auto Forwarded By Knife");
				//message.getMessageInfo().setHighlight("gray");
				return true;
			}
			if (res.getAction().equalsIgnoreCase(Drop)){
				message.setInterceptAction(IInterceptedProxyMessage.ACTION_DROP);
				message.getMessageInfo().setComment("Auto Dropped by Knife");
				message.getMessageInfo().setHighlight("gray");
				return true;
			}
		}
		return false;
	}

}
