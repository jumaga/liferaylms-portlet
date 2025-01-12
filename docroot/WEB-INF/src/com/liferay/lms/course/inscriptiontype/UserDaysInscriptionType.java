package com.liferay.lms.course.inscriptiontype;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.portlet.PortletPreferences;
import javax.portlet.PortletResponse;

import com.liferay.lms.model.Course;
import com.liferay.lms.model.CourseResult;
import com.liferay.lms.service.CourseLocalServiceUtil;
import com.liferay.lms.service.CourseResultLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upload.UploadRequest;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalPreferencesLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.util.PortletKeys;

public class UserDaysInscriptionType extends BaseInscriptionType{
	
	public static final long TYPE = 1;
	
	private static Log log = LogFactoryUtil.getLog(UserDaysInscriptionType.class);
	
	public long getTypeId(){
		return TYPE;
	}
	
	@Override
	public String getSpecificContentPage() {
		return "/html/courseadmin/inscriptiontype/baseinscription.jsp";
	}

	@Override
	public String setExtraContent(UploadRequest uploadRequest,PortletResponse portletResponse, Course course) {
		log.debug("*******setExtraContent**********");
		try {
			String value=ParamUtil.getString(uploadRequest, "inscriptionDays", "");
			
			if(Validator.isNotNull(value)){
				log.debug("****inscriptionDays:"+value);
				
				PortletPreferences prefs= PortalPreferencesLocalServiceUtil.getPreferences(course.getCompanyId(), course.getGroupCreatedId(), PortletKeys.PREFS_OWNER_TYPE_COMPANY);
	
				if(!prefs.isReadOnly("inscription-days")){
					prefs.setValue("inscription-days", value);
					prefs.store();
				}
			}else{
				return "inscription-days-required";
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "error";
		}
		
		return null;
	}
	
	@Override
	public String enrollUser(long courseId, long userId, long teamId, ServiceContext serviceContext) throws PortalException, SystemException {
		Course course = CourseLocalServiceUtil.getCourse(courseId);
		int days = PrefsPropsUtil.getInteger(course.getGroupCreatedId(), "inscription-days");
		Date allowStartDate = new Date();
		Calendar allowDate = Calendar.getInstance();
		allowDate.setTime(allowStartDate);
		allowDate.add(Calendar.DAY_OF_YEAR, days);
		Date allowEndDate = allowDate.getTime();
		
		CourseLocalServiceUtil.addStudentToCourseWithDates(courseId, userId, allowStartDate, allowEndDate);
		
		return "ok";
	}

	@Override
	public String getTitle(Locale locale) {
		return LanguageUtil.get(locale, "inscription-type.user-days.title");
	}
	
	@Override
	public boolean canUnsubscribe() {
		return false;
	}
	
	@Override
	public Set<Integer> getGroupTypesAvailable(){
		Set<Integer> sites = new HashSet<Integer>();
		sites.add(GroupConstants.TYPE_SITE_OPEN);
		return sites;
	}
	
	@Override
	public boolean isActive(long companyId){
		return true;
	}

	@Override
	public String getAllowedTime(long courseId, long userId, Locale locale) {
		String message = StringPool.BLANK;
		try {
			CourseResult courseResult = CourseResultLocalServiceUtil.getCourseResultByCourseAndUser(courseId, userId);
			Course course = CourseLocalServiceUtil.fetchCourse(courseId);		
			
			boolean isSubscribed = GroupLocalServiceUtil.hasUserGroup(userId, course.getGroupCreatedId());			
			if (isSubscribed) {			    
			    if (Validator.isNotNull(courseResult) && Validator.isNotNull(courseResult.getAllowFinishDate())) {
	                
	                Date allowFinishDate = courseResult.getAllowFinishDate();
	                Date nowDate = new Date();
	                
	                long time = allowFinishDate.getTime() - nowDate.getTime();
	                
	                if (time > 0) {
	                    long daysCount = time / (24 * 60 * 60 * 1000);
	                    if (daysCount == 0) {
	                        long hoursCount = time / (60 * 60 * 1000);
	                        if (hoursCount > 1) {
	                            message = LanguageUtil.format(locale, "inscriptioncommunity.allowed-time-hours", hoursCount);
	                        }
	                        else {
	                            message = LanguageUtil.format(locale, "inscriptioncommunity.allowed-time-hour", hoursCount);
	                        }                       
	                    } else {
	                        if (daysCount > 1) {
	                            message = LanguageUtil.format(locale, "inscriptioncommunity.allowed-time-days", daysCount);
	                        }
	                        else {
	                            message = LanguageUtil.format(locale, "inscriptioncommunity.allowed-time-day", daysCount);
	                        }                       
	                    }
	                } else {
	                    // Ya finalizo el tiempo, si no ha aprobado => Opcion inscribir
	                    message = LanguageUtil.get(locale, "inscriptioncommunity.allowed-time-finish");
	                }
	                
	            }
			    else {
			        // no ha empezado el curso
	                
	                if (Validator.isNotNull(course)) {
	                    int days = PrefsPropsUtil.getInteger(course.getGroupCreatedId(), "inscription-days");
	                    message = LanguageUtil.format(locale, "inscriptioncommunity.allowed-time-not-started", days);
	                }			        
			    }			    
			} else {				
				// Aun no esta inscrito en el curso
				if (Validator.isNotNull(course)) {
					int days = PrefsPropsUtil.getInteger(course.getGroupCreatedId(), "inscription-days");
					message = LanguageUtil.format(locale, "inscriptioncommunity.allowed-time-info", days);
				}
				
			}
			
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return message;
	}
}
