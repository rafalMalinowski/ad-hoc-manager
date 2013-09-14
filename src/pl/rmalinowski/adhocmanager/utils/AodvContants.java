package pl.rmalinowski.adhocmanager.utils;

public class AodvContants {
	public static final Integer TTL_VALUE = 3;
	public static final Integer ACTIVE_ROUTE_TIMEOUT = 3000; //3 sekundy
	public static final Integer NET_TRAVERSAL_TIME = 1000; // 1 sekunda
	public static final Integer RREQ_RETRIES = 3;
	public static final Integer PATH_DISCOVERY_TIME = 2 * NET_TRAVERSAL_TIME;
	
	public static final Boolean DEFAULT_G_FLAG_IN_RREQ_VALUE = false;
	
	public static final Long NIEGHBOUR_ACTIVE_ROUTE_TIMEOUT = 1000L * 60L * 60L;
}
