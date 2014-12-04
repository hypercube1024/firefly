package com.firefly.server.http;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ActorFactory {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static ActorSystem actorSystem;
	private static int actorNumber;
	
	public static void init(final Config config) {
		actorSystem = ActorSystem.create(config.getActorRootName());
		actorNumber = config.getActorNumber();
		log.info("actor system root name [{}]", config.getActorRootName());
	}
	
	public static ActorRef getActorRef(Class<? extends Actor> clazz, String name, Object... args) {
		return actorSystem.actorOf(Props.create(clazz, args), name);
	}
	
	public static ActorRef getActorRef(Class<? extends Actor> clazz, String name) {
		return actorSystem.actorOf(Props.create(clazz), name);
	}

	public static ActorSystem getActorSystem() {
		return actorSystem;
	}
	
	public static int getActorNumber() {
		return actorNumber;
	}

	public static void shutdown() {
		actorSystem.shutdown();
	}
}
