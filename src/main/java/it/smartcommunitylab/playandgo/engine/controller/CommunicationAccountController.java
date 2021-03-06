/**
 *    Copyright 2012-2013 Trento RISE
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 *    Copyright 2012-2013 Trento RISE
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package it.smartcommunitylab.playandgo.engine.controller;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.dto.UserSignature;
import it.smartcommunitylab.playandgo.engine.model.Configuration;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.UserAccount;
import it.smartcommunitylab.playandgo.engine.notification.CommunicationManager;
import it.smartcommunitylab.playandgo.engine.notification.Notification;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.UserAccountRepository;


@RestController
public class CommunicationAccountController extends PlayAndGoController {

	@Autowired
	private  UserAccountRepository userRepo;
	
	@Autowired
	private CommunicationManager communicationManager;

	@Autowired
	private CampaignSubscriptionRepository campaignSubscriptionRepository;

	/**
	 * List of current player notifications
	 * @param since, timestamp to start from
	 * @param skip, pagination position
	 * @param limit, pagination count
	 * @return
	 * @throws Exception
	 */
	@GetMapping("/api/app/notifications")
	public @ResponseBody List<Notification> getPlayerNotifications(
			@RequestParam(required = false, defaultValue="0") Long since,
			@RequestParam(required = false, defaultValue = "0") Integer skip,
			@RequestParam(required = false, defaultValue = "20") Integer limit,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		Set<String> campaigns = campaignSubscriptionRepository.findByPlayerId(player.getPlayerId()).stream().map(cs -> cs.getCampaignId()).collect(Collectors.toSet());
		return communicationManager.get(player.getPlayerId(), player.getTerritoryId(), campaigns, since, skip, limit);
	}
	
	/**
	 * Register for push notifications assigning the device-specific registration ID to the user account for 
	 * individual push messages
	 * @param signature
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/api/app/register")
	public @ResponseBody boolean registerUserToPush(
			@RequestBody UserSignature signature, 
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return registerUser(signature, player.getPlayerId());

	}
		
	private boolean registerUser(UserSignature signature, String playerId) throws Exception {
		UserAccount userAccount;
		String registrationId = signature.getRegistrationId();
		// set value of sender/serverside user registration code
		if (registrationId == null) {
			throw new IllegalArgumentException("Missing registration id.");
		}
		userAccount = userRepo.findByPlayerId(playerId);
		if (userAccount == null) {
			userAccount = new UserAccount();
			userAccount.setPlayerId(playerId);
			userRepo.save(userAccount);
		}

		List<Configuration> listConf = userAccount.getConfigurations();
		if (listConf == null) {
			listConf = new LinkedList<Configuration>();
		}
		boolean exists = listConf.stream().anyMatch(c -> registrationId.equals(c.getRegistrationId()));
		if (!exists) {
			Configuration e = new Configuration();
			e.setPlatform(signature.getPlatform());
			e.setRegistrationId(registrationId);
			listConf.add(e);
			userAccount.setConfigurations(listConf);
			userRepo.save(userAccount);
		}

		return true;
	}
	
}
