package com.digitalsanctuary.spring.user.controller;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import com.digitalsanctuary.spring.user.event.AuditEvent;
import com.digitalsanctuary.spring.user.persistence.model.User;
import com.digitalsanctuary.spring.user.service.UserService;
import com.digitalsanctuary.spring.user.service.UserVerificationService;
import com.digitalsanctuary.spring.user.service.UserService.PasswordResetTokenValidationResult;
import com.digitalsanctuary.spring.user.util.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * The UserActionController handles non-API, non-Page requests like token validation links from emails.
 */
@Slf4j
@Controller
public class UserActionController {
	private static final String AUTH_MESSAGE_PREFIX = "auth.message.";

	/** The user service. */
	@Autowired
	private UserService userService;

	@Autowired
	private UserVerificationService userVerificationService;

	/** The messages. */
	@Autowired
	private MessageSource messages;

	/** The event publisher. */
	@Autowired
	private ApplicationEventPublisher eventPublisher;

	// URIs configured in application.properties
	/** The registration pending URI. */
	@Value("${user.security.registrationPendingURI}")
	private String registrationPendingURI;

	/** The registration success URI. */
	@Value("${user.security.registrationSuccessURI}")
	private String registrationSuccessURI;

	/** The registration new verification URI. */
	@Value("${user.security.registrationNewVerificationURI}")
	private String registrationNewVerificationURI;

	/** The forgot password pending URI. */
	@Value("${user.security.forgotPasswordPendingURI}")
	private String forgotPasswordPendingURI;

	/** The forgot password change URI. */
	@Value("${user.security.forgotPasswordChangeURI}")
	private String forgotPasswordChangeURI;

	/**
	 * Validate a forgot password token link from an email, and if valid, show the change password page.
	 *
	 * @param model the model
	 * @param token the token
	 * @return the model and view
	 */
	@GetMapping("/user/changePassword")
	public ModelAndView showChangePasswordPage(final HttpServletRequest request, final ModelMap model, @RequestParam("token") final String token) {
		log.debug("UserAPI.showChangePasswordPage: called with token: {}", token);
		final PasswordResetTokenValidationResult result = userService.validatePasswordResetToken(token);
		log.debug("UserAPI.showChangePasswordPage:" + "result: {}", result);
		AuditEvent changePasswordAuditEvent = new AuditEvent(this, null, request.getSession().getId(), UserUtils.getClientIP(request),
				request.getHeader("User-Agent"), "showChangePasswordPage", "Success", "Requested. Result:" + result, null);
		eventPublisher.publishEvent(changePasswordAuditEvent);
		if (PasswordResetTokenValidationResult.VALID.equals(result)) {
			model.addAttribute("token", token);
			String redirectString = "redirect:" + forgotPasswordChangeURI;
			return new ModelAndView(redirectString, model);
		} else {
			String messageKey = AUTH_MESSAGE_PREFIX + result.name().toLowerCase();
			model.addAttribute("messageKey", messageKey);
			return new ModelAndView("redirect:/index.html", model);
		}
	}

	/**
	 * Validate a forgot password token link from an email, and if valid, show the registration success page.
	 *
	 * @param request the request
	 * @param model the model
	 * @param token the token
	 * @return the model and view
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@GetMapping("/user/registrationConfirm")
	public ModelAndView confirmRegistration(final HttpServletRequest request, final ModelMap model,
			@RequestParam("token") final String token) throws UnsupportedEncodingException {
		log.debug("UserAPI.confirmRegistration: called with token: {}", token);
		Locale locale = request.getLocale();
		model.addAttribute("lang", locale.getLanguage());
		final String result = userVerificationService.validateVerificationToken(token);
		if (result.equals("valid")) {
			final User user = userVerificationService.getUserByVerificationToken(token);
			if (user != null) {
				userService.authWithoutPassword(user);
				userVerificationService.deleteVerificationToken(token);

				AuditEvent registrationAuditEvent = new AuditEvent(this, user, request.getSession().getId(), UserUtils.getClientIP(request),
						request.getHeader("User-Agent"), "Registration Confirmation", "Success", "Registration Confirmed. User logged in.", null);
				eventPublisher.publishEvent(registrationAuditEvent);
			}

			model.addAttribute("message", messages.getMessage("message.accountVerified", null, locale));
			log.debug("UserAPI.confirmRegistration: account verified and user logged in!");
			String redirectString = "redirect:" + registrationSuccessURI;
			return new ModelAndView(redirectString, model);
		}

		model.addAttribute("messageKey", AUTH_MESSAGE_PREFIX + result);
		model.addAttribute("expired", "expired".equals(result));
		model.addAttribute("token", token);
		log.debug("UserAPI.confirmRegistration: failed.  Token not found or expired.");
		String redirectString = "redirect:" + registrationNewVerificationURI;
		return new ModelAndView(redirectString, model);
	}
}
