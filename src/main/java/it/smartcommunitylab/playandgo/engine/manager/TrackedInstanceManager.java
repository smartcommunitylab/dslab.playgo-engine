package it.smartcommunitylab.playandgo.engine.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;

@Component
public class TrackedInstanceManager {

	@Autowired
	private TrackedInstanceRepository trackedInstanceRepository;
	
	
}
