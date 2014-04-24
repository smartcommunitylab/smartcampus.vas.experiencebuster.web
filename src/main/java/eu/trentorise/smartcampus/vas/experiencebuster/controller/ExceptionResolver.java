package eu.trentorise.smartcampus.vas.experiencebuster.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import eu.trentorise.smartcampus.profileservice.ProfileServiceException;

public class ExceptionResolver extends DefaultHandlerExceptionResolver {

	private static final Logger logger = Logger
			.getLogger(ExceptionResolver.class);

	@Override
	protected ModelAndView doResolveException(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex) {

		if (ex instanceof SecurityException) {
			try {
				return resolveSecurityException(response, ex);
			} catch (IOException e) {
				logger.error("Exception resolving SecurityExcetion");
			}
		} else if (ex instanceof ProfileServiceException) {
			logger.error("Profileservice exception occurred");
			try {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
						ex.getMessage());
			} catch (IOException e) {
				logger.error("Exception resolving ProfileserviceException");
			}
			return new ModelAndView();
		}

		return super.doResolveException(request, response, handler, ex);
	}

	private ModelAndView resolveSecurityException(HttpServletResponse response,
			Exception exception) throws IOException {
		logger.error("Security exception occurred");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				exception.getMessage());
		return new ModelAndView();
	}
}
