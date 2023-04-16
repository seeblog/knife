package knife;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IContextMenuInvocation;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.Utils;
import config.ConfigEntry;
import manager.DismissedTargetsManager;

public class DismissMenu extends JMenuItem {//JMenuItem vs. JMenu

	public DismissMenu(BurpExtender burp){
		this.setText("^_^ Dismissed");
		this.addActionListener(new Dismiss_Action(burp,burp.invocation));
	}
}

class Dismiss_Action implements ActionListener{
	//scope matching is actually String matching!!
	private IContextMenuInvocation invocation;
	public BurpExtender myburp;
	public IExtensionHelpers helpers;
	public PrintWriter stdout;
	public PrintWriter stderr;
	public IBurpExtenderCallbacks callbacks;
	//callbacks.printOutput(Integer.toString(invocation.getToolFlag()));//issue tab of target map is 16
	public Dismiss_Action(BurpExtender burp,IContextMenuInvocation invocation) {
		this.invocation  = invocation;
		this.myburp = burp;
		this.helpers = burp.helpers;
		this.callbacks = BurpExtender.callbacks;
		this.stderr = burp.stderr;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		String action = fetchChangeType();

		IHttpRequestResponse[] messages = invocation.getSelectedMessages();
		
		String keyword = null;
		if (action == ConfigEntry.Action_Drop_Request_If_Keyword_Matches || 
				action == ConfigEntry.Action_Forward_Request_If_Keyword_Matches) {
			keyword = fetchKeyword();
		}
		
		DismissedTargetsManager.putRule(messages, keyword, action);
	}

	public static String fetchChangeType() {
		/*
		Object[] options = { "Help","Drop Host","Drop URL","Drop Keyword","Forward Host","Forward URL","Forward Keyword"};
		int user_input = JOptionPane.showOptionDialog(null, "Which Action Do You Want To Take?", "Chose Your Action And Scope",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
				null, options, options[0]);
		 */

		String[] options = new ConfigEntry().listAllDropForwardActions();
		String selectedValue = (String) JOptionPane.showInputDialog(null,
				"Chose Your Action", "Chose Action To Handle",
				JOptionPane.INFORMATION_MESSAGE, null,
				options, options[0]);

		if (selectedValue == options[options.length-1]) {
			try {
				Utils.browserOpen("https://github.com/bit4woo/knife/blob/master/Help.md", null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			selectedValue = fetchChangeType();
		}
		return selectedValue;
	}


	public static String fetchKeyword() {
		String keyword = JOptionPane.showInputDialog("Input Your Keyword", "");
		if (keyword == null) {
			return null;
		}
		return keyword.trim();
	}
}