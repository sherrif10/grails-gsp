/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.sitemesh;

import groovy.text.Template;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.view.AbstractGrailsView;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

import com.opensymphony.module.sitemesh.RequestConstants;
import com.opensymphony.sitemesh.Content;

public class GrailsLayoutView extends AbstractGrailsView {
    GroovyPageLayoutFinder groovyPageLayoutFinder;
    
    protected View innerView;

    public static final String GSP_SITEMESH_PAGE = GrailsLayoutView.class.getName() + ".GSP_SITEMESH_PAGE";
    
    public GrailsLayoutView(GroovyPageLayoutFinder groovyPageLayoutFinder, View innerView) {
        this.groovyPageLayoutFinder = groovyPageLayoutFinder;
        this.innerView = innerView;
    }
    
    @Override
    public String getContentType() {
        return MediaType.ALL_VALUE;
    }

    @Override
    protected void renderTemplate(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Content content = obtainContent(model, webRequest, request, response);
        if (content != null) {
            beforeDecorating(content, model, webRequest, request, response);
            if(!WebUtils.isIncludeRequest(request)) {
                SpringMVCViewDecorator decorator = (SpringMVCViewDecorator) groovyPageLayoutFinder.findLayout(request, content);
                if (decorator != null) {
                    decorator.render(content, model, request, response, webRequest.getServletContext());
                    return;
                }
            }
			PrintWriter writer = response.getWriter();
			content.writeOriginal(writer);
			if (!response.isCommitted()) {
				writer.flush();
			}
        }
    }

    protected void beforeDecorating(Content content, Map<String, Object> model, GrailsWebRequest webRequest,
            HttpServletRequest request, HttpServletResponse response) {
        applyMetaHttpEquivContentType(content, response);
    }

    protected void applyMetaHttpEquivContentType(Content content, HttpServletResponse response) {
        String contentType = content.getProperty("meta.http-equiv.Content-Type");
        if (contentType != null && "text/html".equals(response.getContentType())) {
            response.setContentType(contentType);
        }
    }

    protected Content obtainContent(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Object oldPage = request.getAttribute(RequestConstants.PAGE);
        request.removeAttribute(RequestConstants.PAGE);
        Object oldGspSiteMeshPage=request.getAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE);
        HttpServletResponse previousResponse = webRequest.getWrappedResponse();
        HttpServletResponse previousWrappedResponse = WrappedResponseHolder.getWrappedResponse();
        try {
            request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, new GSPSitemeshPage());
            
            GrailsContentBufferingResponse contentBufferingResponse = createContentBufferingResponse(model, webRequest, request, response);
            webRequest.setWrappedResponse(contentBufferingResponse);
            WrappedResponseHolder.setWrappedResponse(contentBufferingResponse);
            
            renderInnerView(model, webRequest, request, response, contentBufferingResponse);
            
            return contentBufferingResponse.getContent();
        }
        finally {
            if (oldGspSiteMeshPage != null) {
                request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, oldGspSiteMeshPage);
            }
            if (oldPage != null) {
                request.setAttribute(RequestConstants.PAGE, oldPage);
            }
            webRequest.setWrappedResponse(previousResponse);
            WrappedResponseHolder.setWrappedResponse(previousWrappedResponse);
        }
    }

    protected void renderInnerView(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request,
            HttpServletResponse response,
            GrailsContentBufferingResponse contentBufferingResponse) throws Exception {
        innerView.render(model, request, contentBufferingResponse);
    }

    protected GrailsContentBufferingResponse createContentBufferingResponse(Map<String, Object> model, GrailsWebRequest webRequest, HttpServletRequest request,
            HttpServletResponse response) {
        return new GrailsViewBufferingResponse(request, response, getServletContext());
    }

    @Override
    public Template getTemplate() {
        if(innerView instanceof AbstractGrailsView) {
            return ((AbstractGrailsView)innerView).getTemplate();
        }
        return null;
    }

    public View getInnerView() {
        return innerView;
    }
}
