package org.red5.sip.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zoolu.net.SocketAddress;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;

public abstract class SIPTransport implements SIPUserAgentListener,
		SIPRegisterAgentListener, ISipNumberListener {
	protected static Logger log = LoggerFactory.getLogger(SIPTransport.class);

	protected RTMPRoomClient roomClient;
	private SipProvider sip_provider;
	private SIPUserAgentProfile user_profile;
	private String opt_outbound_proxy = null;
	private SIPUserAgent ua;
	private SIPRegisterAgent ra;

	private String username;
	private String password;
	private int sipPort;
	private int rtpPort;
	private String proxy;
	private String number;

	private void p(String s) {
		log.debug(s);
	}

	public SIPTransport(RTMPRoomClient roomClient, int sipPort, int rtpPort) {
		this.roomClient = roomClient;
		this.sipPort = sipPort;
		this.rtpPort = rtpPort;
	}

	public void login(String obproxy, String phone, String username,
			String password, String realm, String proxy) {
		p("login");

		this.username = username;
		this.password = password;
		this.proxy = proxy;
		this.opt_outbound_proxy = obproxy;

		String fromURL = "\"" + phone + "\" <sip:" + phone + "@" + proxy + ">";

		try {
			SipStack.init();
			SipStack.debug_level = 0;
			SipStack.log_path = "log";

			sip_provider = new SipProvider(null, sipPort);
			sip_provider
					.setOutboundProxy(new SocketAddress(opt_outbound_proxy));

			user_profile = new SIPUserAgentProfile();
			user_profile.audioPort = rtpPort;
			user_profile.username = username;
			user_profile.passwd = password;
			user_profile.realm = realm;
			user_profile.fromUrl = fromURL;
			user_profile.contactUrl = "sip:" + phone + "@"
					+ sip_provider.getViaAddress();

			if (sip_provider.getPort() != SipStack.default_port) {
				user_profile.contactUrl += ":" + sip_provider.getPort();
			}

			user_profile.keepaliveTime = 8000;
			user_profile.acceptTime = 0;
			user_profile.hangupTime = 20;

			ua = new SIPUserAgent(sip_provider, user_profile, this, roomClient);

			ua.listen();

		} catch (Exception e) {
			p("login: Exception:>\n" + e);
		}
	}

	public void call(String destination) {
		p("Calling " + destination);

		try {
			roomClient.init();

			ua.setMedia(roomClient);
			ua.hangup();

			if (destination.indexOf("@") == -1) {
				destination = destination + "@" + proxy;
			}

			if (destination.indexOf("sip:") > -1) {
				destination = destination.substring(4);
			}

			ua.call(destination);
		} catch (Exception e) {
			p("call: Exception:>\n" + e);
		}
	}

	public void register() {
		p("register");
		roomClient.stop();

		try {

			if (sip_provider != null) {
				ra = new SIPRegisterAgent(sip_provider, user_profile.fromUrl,
						user_profile.contactUrl, username, user_profile.realm,
						password, this);
				loopRegister(user_profile.expires, user_profile.expires / 2,
						user_profile.keepaliveTime);
			}

		} catch (Exception e) {
			p("register: Exception:>\n" + e);
		}
	}

	public void close() {
		p("close");

		try {
			hangup();
		} catch (Exception e) {
			p("close: Exception:>\n" + e);
		}

		try {
			p("provider.halt");
			sip_provider.halt();
		} catch (Exception e) {
			p("close: Exception:>\n" + e);
		}

		try {
			unregister();
		} catch (Exception e) {
			p("close: Exception:>\n" + e);
		}
	}

	public void hangup() {
		p("hangup");

		if (ua != null) {
			if (!ua.call_state.equals(SIPUserAgent.UA_IDLE)) {
				ua.hangup();
				ua.listen();
			}
		}

		closeStreams();
		roomClient.stop();
	}

	private void closeStreams() {
		p("closeStreams");
	}

	public void unregister() {
		p("unregister");

		if (ra != null) {
			if (ra.isRegistering()) {
				ra.halt();
			}
			ra.unregister();
			ra = null;
		}

		if (ua != null) {
			ua.hangup();
		}
		ua = null;
	}

	private void loopRegister(int expire_time, int renew_time, long keepalive_time) {
		if (ra.isRegistering()) {
			ra.halt();
		}
		ra.loopRegister(expire_time, renew_time, keepalive_time);
	}

	public void onUaCallIncoming(SIPUserAgent ua, NameAddress callee, NameAddress caller) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	public void onUaCallCancelled(SIPUserAgent ua) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	public void onUaCallRinging(SIPUserAgent ua) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	public void onUaCallAccepted(SIPUserAgent ua) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	public void onUaCallTrasferred(SIPUserAgent ua) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	public void onUaCallFailed(SIPUserAgent ua) {
		log.info("Call failed");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.info("Reconnection pause was interrupted");
		}
		roomClient.start();
	}

	public void onUaCallClosing(SIPUserAgent ua) {
		log.info("Call closing");
	}

	public void onUaCallClosed(SIPUserAgent ua) {
		log.info("Call closed");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.info("Reconnection pause was interrupted");
		}
		log.info("Try reconnect: Call " + number);
		register();
	}

	public void onUaCallConnected(SIPUserAgent ua) {
		log.info("Call connected");
	}

	public void onSipNumber(String number) {
		log.info("Room number: " + number);
		this.number = number;
		this.call(number);
	}
}