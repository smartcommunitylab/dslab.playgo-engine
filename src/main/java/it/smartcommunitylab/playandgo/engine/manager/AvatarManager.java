package it.smartcommunitylab.playandgo.engine.manager;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.model.Avatar;
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
	
	public Avatar getPlayerAvatar(String playerId) {
		return avatarRepository.findByPlayerId(playerId);
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
		Binary bb = new Binary(cb);
		Binary bbs = new Binary(cbs);
		avatar.setAvatarData(bb);
		avatar.setAvatarDataSmall(bbs);
		avatar.setContentType(data.getContentType());
		avatar.setFileName(data.getOriginalFilename());
		avatarRepository.save(avatar);
		return avatar;
	}
	
}
