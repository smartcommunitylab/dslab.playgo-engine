package it.smartcommunitylab.playandgo.engine.config;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import it.smartcommunitylab.playandgo.engine.util.Utils;

public class DateDeserializer extends StdDeserializer<Date> {
	private static final long serialVersionUID = 1L;

	protected DateDeserializer() {
		super(Date.class);
	}

	public Date deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		return Utils.getUTCDate(jp.getLongValue());
	}

}
