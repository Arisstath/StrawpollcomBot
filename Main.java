package me.arisstath.strawcom;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Main {

	public static AtomicInteger aip;
	public static ArrayList<String> proxies = new ArrayList<>();

	public static String getProxy() {
		try {
			return proxies.get(aip.getAndIncrement());
		} catch (Exception e) {
			aip = new AtomicInteger(0);
			return getProxy();
		}
	}

	public static String getOids(int choice, String url) throws IOException{
		Connection connection = Jsoup.connect("https://strawpoll.com/" + url);
		Document doc = connection.get();
		int choicee = 0;
		for(Element el : doc.getElementsByClass("checkbox-danger")){
			choicee++;
			System.out.println("Choice #" + choicee);
			if(choicee == choice){
				System.out.println("Parsed element was " + el.getElementsByTag("label").get(0).text());
				return el.getElementsByTag("input").get(0).attr("name");
			}
		}
		return "";
	}
	public static void vote(String pid, String oids,String host, int port) {
		Connection connection = Jsoup.connect("https://strawpoll.com/vote");
		connection.ignoreHttpErrors(true);
		connection.ignoreContentType(true);
		connection.proxy(new Proxy(Type.SOCKS, new InetSocketAddress(host, port)));
		connection.data("pid", pid);
		connection.data("oids", oids);
		connection.header("user-agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.96 Safari/537.36");
		connection.header("referer", "https://strawpoll.com/"+pid);
		connection.header("X-Requested-With", "XMLHttpRequest");
		connection.header("origin", "https://strawpoll.com");
		connection.header("Host", "strawpoll.com");

		Document doc = null;
		try {
			doc = connection.post();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (doc.toString().contains("already voted")) {
			System.out.println(Thread.currentThread().getId() + " Vote failed. Already voted");
		} else {
			System.out.println(Thread.currentThread().getId() + " Vote successed.");
		}
	}

	public static void main(String[] args) throws IOException {
		if(args.length != 2){
			System.out.println("Incorrect usage, correct usage java -jar strawpoll.jar <choice order> <vote id>");
			return;
		}
		String oids = getOids(Integer.parseInt(args[0]), args[1]);
		if(oids.isEmpty() || oids.equals("")){
			System.out.println("Could not find the specific choice, try again.");
			return;
		}
		System.out.println("Loading proxies...");
		Scanner scanner = new Scanner(new File("proxies.txt"));
		while (scanner.hasNextLine()) {
			proxies.add(scanner.nextLine());
		}
		scanner.close();
		System.out.println("Loaded " + proxies.size() + " proxies.");
	
		ExecutorService executor = Executors.newFixedThreadPool(300);
		for(int i = 0; i < proxies.size(); i++)
		executor.execute(new Runnable() {
			@Override
			public void run() {
				String proxy = getProxy();
				vote(args[1],oids,proxy.split(":")[0], Integer.parseInt(proxy.split(":")[1]));
			}

		});

	}
}
