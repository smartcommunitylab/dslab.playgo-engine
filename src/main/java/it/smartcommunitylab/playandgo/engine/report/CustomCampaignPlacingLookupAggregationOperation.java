package it.smartcommunitylab.playandgo.engine.report;

import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;

public class CustomCampaignPlacingLookupAggregationOperation implements AggregationOperation {
	private String jsonOperation;
    
	public CustomCampaignPlacingLookupAggregationOperation(String jsonOperation) {
        this.jsonOperation = jsonOperation;
    }
	
	@Override
	public Document toDocument(AggregationOperationContext aggregationOperationContext) {
		return aggregationOperationContext.getMappedObject(Document.parse(jsonOperation));
	}

}
