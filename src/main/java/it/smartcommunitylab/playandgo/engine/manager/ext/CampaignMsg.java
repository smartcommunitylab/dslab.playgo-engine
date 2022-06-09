package it.smartcommunitylab.playandgo.engine.manager.ext;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;

@Document(collection="campaignMessages")
public class CampaignMsg {
	public static enum Operation {
		validate, invalidate, update
	};
	

	@Id
	private String id;

	private Type type;
	private Operation operation;
	@Indexed
	private String campaignPlayerTrackId;
	private Date creationTime;
	private Date updateTime;
	private String errorMsg;
	private String errorCode;
	private Object msg;
	
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public String getCampaignPlayerTrackId() {
		return campaignPlayerTrackId;
	}
	public void setCampaignPlayerTrackId(String campaignPlayerTrackId) {
		this.campaignPlayerTrackId = campaignPlayerTrackId;
	}
	public Date getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}
	public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public Object getMsg() {
		return msg;
	}
	public void setMsg(Object msg) {
		this.msg = msg;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
