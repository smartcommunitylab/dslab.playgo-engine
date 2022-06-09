package it.smartcommunitylab.playandgo.engine.manager.ext;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.manager.ext.CampaignMsg.Operation;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.mq.UpdateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignMsgRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class CampaignMsgManager {
	private static transient final Logger logger = LoggerFactory.getLogger(CampaignMsgManager.class);
	
	@Autowired
	CampaignMsgRepository campaignMsgRepository;
	
	private void addRequest(String campaignPlayerTrackId, Type type, Operation operation, 
			String errorMsg, String errorCode, Object msg) {
		CampaignMsg campaignMsg = campaignMsgRepository.findByCampaignPlayerTrackIdAndOperation(campaignPlayerTrackId, 
				operation);
		if(campaignMsg != null) {
			campaignMsgRepository.delete(campaignMsg);
		}
		campaignMsg = new CampaignMsg();
		campaignMsg.setType(type);
		campaignMsg.setOperation(operation);
		campaignMsg.setCampaignPlayerTrackId(campaignPlayerTrackId);
		Date utcDate = Utils.getUTCDate(System.currentTimeMillis());
		campaignMsg.setCreationTime(utcDate);
		campaignMsg.setUpdateTime(utcDate);
		campaignMsg.setErrorMsg(errorMsg);
		campaignMsg.setErrorCode(errorCode);
		campaignMsg.setMsg(msg);
		campaignMsgRepository.save(campaignMsg);
	}
	
	public void addValidateTripRequest(ValidateCampaignTripRequest msg, Type type, String errorMsg, String errorCode) {
		addRequest(msg.getCampaignPlayerTrackId(), type, Operation.validate, errorMsg, errorCode, msg);
	}
	
	public void addInvalidateTripRequest(ValidateCampaignTripRequest msg, Type type, String errorMsg, String errorCode) {
		addRequest(msg.getCampaignPlayerTrackId(), type, Operation.invalidate, errorMsg, errorCode, msg);
	}
	
	public void addUpdateTripRequest(UpdateCampaignTripRequest msg, Type type, String errorMsg, String errorCode) {
		addRequest(msg.getCampaignPlayerTrackId(), type, Operation.update, errorMsg, errorCode, msg);
	}
}
