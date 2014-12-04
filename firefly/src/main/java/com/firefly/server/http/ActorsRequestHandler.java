package com.firefly.server.http;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ActorsRequestHandler extends RequestHandler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private ActorRef[] actorRefs;
	private AtomicInteger index = new AtomicInteger(0);
	
	public ActorsRequestHandler(HttpServletDispatcherController servletController) {
		super(servletController);
		int level = 1;
		while (level < ActorFactory.getActorNumber()) {
			level <<= 1;
		}
		log.info("actor number of request handler [{}]", level);
		actorRefs = new ActorRef[level];
		for (int i = 0; i < actorRefs.length; i++) {
			actorRefs[i] = ActorFactory.getActorRef(RequestProcessingActor.class, "requestProcessingActor" + i, this);
		}
	}

	@Override
	public void doRequest(Session session, HttpServletRequestImpl request) throws IOException {
		if (request.response.system) { // response HTTP decode error
			request.response.outSystemData();
		} else {
			actorRefs[index.getAndIncrement() & actorRefs.length].tell(request, ActorRef.noSender());
		}
	}

	@Override
	public void shutdown() {
		ActorFactory.shutdown();
	}
	
	public static class RequestProcessingActor extends UntypedActor {
		
		private ActorsRequestHandler actorsRequestHandler;

		public RequestProcessingActor(ActorsRequestHandler actorsRequestHandler) {
			this.actorsRequestHandler = actorsRequestHandler;
		}

		@Override
		public void onReceive(Object message) throws Exception {
			if(message instanceof HttpServletRequestImpl) {
				actorsRequestHandler.doRequest((HttpServletRequestImpl)message);
			} else {
				unhandled(message);
			}
		}

	}

}
