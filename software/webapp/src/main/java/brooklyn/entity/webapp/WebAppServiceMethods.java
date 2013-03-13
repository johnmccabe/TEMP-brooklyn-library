package brooklyn.entity.webapp;

import brooklyn.enricher.RollingTimeWindowMeanEnricher;
import brooklyn.enricher.TimeWeightedDeltaEnricher;
import brooklyn.entity.basic.EntityLocal;

public class WebAppServiceMethods implements WebAppServiceConstants {
    
    public static void connectWebAppServerPolicies(EntityLocal entity) {
        entity.addEnricher(TimeWeightedDeltaEnricher.<Integer>getPerSecondDeltaEnricher(entity,
                WebAppServiceConstants.REQUEST_COUNT, WebAppServiceConstants.REQUESTS_PER_SECOND_LAST));
        
        entity.addEnricher(new RollingTimeWindowMeanEnricher<Double>(entity,
            WebAppServiceConstants.REQUESTS_PER_SECOND_LAST, WebAppServiceConstants.REQUESTS_PER_SECOND_IN_WINDOW,
            WebAppServiceConstants.REQUESTS_PER_SECOND_WINDOW_PERIOD));
    }
}
