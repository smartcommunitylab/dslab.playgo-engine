package it.smartcommunitylab.playandgo.engine.manager;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.model.Avatar;
import it.smartcommunitylab.playandgo.engine.model.Image;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.AvatarRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.ImageUtils;

@Component
public class AvatarManager {
	private static transient final Logger logger = LoggerFactory.getLogger(AvatarManager.class);
	
	private final static int DIMENSION = 640;
	private final static int DIMENSION_SMALL = 160;
	
	@Autowired
	AvatarRepository avatarRepository;
	
	@Autowired
	StorageManager storageManager;
	
	public Avatar getPlayerAvatar(String playerId) {
		return avatarRepository.findByPlayerId(playerId);
	}
	
	public Image getPlayerSmallAvatar(String playerId) {
		Avatar avatar = avatarRepository.findByPlayerId(playerId);
		if(avatar != null) {
			Image image = new Image();
			image.setContentType(avatar.getContentType());
			image.setUrl(avatar.getAvatarSmallUrl());
			return image;
		}
		return null;
	}
	
	public Avatar uploadPlayerAvatar(Player player, MultipartFile data) throws Exception {
		if (data.getSize() > 10 * 1024 * 1024) {
			logger.warn("Image too big.");
			throw new BadRequestException("image too big", ErrorCode.IMAGE_TOO_BIG);
		}
		MediaType mediaType = MediaType.parseMediaType(data.getContentType());
		if (!mediaType.isCompatibleWith(MediaType.IMAGE_GIF) && !mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) && !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
			logger.warn("Image format not supported");
			throw new BadRequestException("Image format not supported", ErrorCode.IMAGE_WRONG_FORMAT);
		}
		Avatar avatar = getPlayerAvatar(player.getPlayerId());
		if(avatar == null) {
			avatar = new Avatar();
			avatar.setPlayerId(player.getPlayerId());
		}
		BufferedImage bs = ImageIO.read(data.getInputStream());
		byte cb[] = ImageUtils.compressImage(bs, data.getContentType(), DIMENSION);
		byte cbs[] = ImageUtils.compressImage(bs, data.getContentType(), DIMENSION_SMALL);
		String avatarImage = "avatar-" + player.getPlayerId();
		String avatarSmallImage = "avatar-small-" + player.getPlayerId();
		try {
			String avatarUrl = storageManager.uploadImage(avatarImage, data.getContentType(), cb);
			avatar.setAvatarUrl(avatarUrl);
			String avatarSmallUrl = storageManager.uploadImage(avatarSmallImage, data.getContentType(), cbs);
			avatar.setAvatarSmallUrl(avatarSmallUrl);
			avatar.setContentType(data.getContentType());
			avatarRepository.save(avatar);
		} catch (Exception e) {
			throw new ServiceException("Error storing image", ErrorCode.IMAGE_STORE_ERROR);
		}
		return avatar;
	}
	
	public void deleteAvatar(String playerId) throws Exception {
		Avatar avatar = avatarRepository.findByPlayerId(playerId);
		if(avatar != null) {
			String avatarImage = "avatar-" + playerId;
			String avatarSmallImage = "avatar-small-" + playerId;
			storageManager.deleteImage(avatarImage);
			storageManager.deleteImage(avatarSmallImage);
			avatarRepository.delete(avatar);
		}
	}
	
}
