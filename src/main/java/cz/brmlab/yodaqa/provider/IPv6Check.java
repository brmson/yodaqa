package cz.brmlab.yodaqa.provider;

import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Enable IPv6 if it works.  Java (or Apache HTTP commons) is glitchy in that
 * either it uses IPv4 or IPv6, but it is unable to gracefully fall back to
 * IPv4 if IPv6 connection fails (with No route to host, typically).  On the
 * other hand, on our experimental machines, we very much do want to use IPv6
 * as IPv4 will cause NAT traversals which are very expensive with our typical
 * number of database connections.
 *
 * This code is based on hobaberra's
 *
 * 	https://gist.github.com/hobarrera/319ce21a26fb4594e2e2
 *
 */
public class IPv6Check {
	/**
	 * Enables IPv6 only if it works. Returns true if IPv6 was enabled.
	 * Upon any sort of error will bail and disable the IPv6 preference.
	 */
	public static boolean enableIPv6IfItWorks() {
		try {
			// This flag is set to true after an IPv6 connection was
			// established.
			// Temporarily undo this setting. Altering preferIPv6Address will
			// suffice
			for (InetAddress addr : InetAddress.getAllByName("ipv6.ailao.eu")) {
				// InetAddress#isReachable would fail in some special scenarios
				// (ie: filtered ICMP)
				if (addr instanceof Inet4Address)
					continue;
				if (addr instanceof Inet6Address) {
					new Socket(addr, 80);
				}
			}
			System.err.println("Preferring IPv6 connections");
			System.setProperty("java.net.preferIPv6Addresses", "true");
			return true;
		} catch (Exception e) {
			// Catching Exception isn't generally a good idea, but in this
			// scenario we want to bail regardless of what the error was. The
			// purpose is to not add any new possible points of failure.
			System.err.println("Preferring IPv4 connections");
			System.setProperty("java.net.preferIPv6Addresses", "false");
		}
		return false;
	}
}
