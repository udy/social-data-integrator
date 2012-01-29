/*
   Copyright 2011 Udayakumar Dhansingh (Udy)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   
 */
package org.onesun.smc.core.services.rest;

import java.util.Map;

import org.apache.log4j.Logger;
import org.onesun.smc.api.ConnectionProperties;
import org.onesun.smc.api.ServiceProvider;
import org.onesun.smc.api.SocialMediaProvider;
import org.onesun.smc.core.connection.properties.SocialMediaConnectionProperties;
import org.onesun.smc.core.resources.RESTResource;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Request;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public class RestListener {
	private static Logger logger = Logger.getLogger(RestListener.class);
	
	private String callbackUrl = null;
	private RESTResource resource = null;
	private String authentication = "NONE";
	
	private ConnectionProperties connection = null;
	private OAuthService service = null;
	private Token accessToken = null;
	
	private int responseCode = -1;
	private String responseBody = null;
	private ServiceProvider serviceProvider = null;
	
	public RestListener(ServiceProvider serviceProvider, RESTResource resource, String authentication, String callbackUrl){
		// Generic
		this.serviceProvider = serviceProvider;
		this.resource = resource;
		this.authentication = authentication;
		this.callbackUrl = callbackUrl;
	}
	
	public void execute(){
		// Substitute URL parameters if required
		String parameters = resource.getParameters();
		String url = resource.getUrl();
		
		if(parameters != null && parameters.trim().length() > 0){
			if(url.contains("?")){
				parameters = parameters.replace("?", "&");
			}
			
			url += parameters;
		}
		
		Request request = null;
		
		// Sign with access token if required
		if(authentication.compareTo("OAUTH") == 0  && resource.isAccessTokenRequired() == true){
			request = new OAuthRequest(resource.getVerb(), url);
			
			// Assume that authorizer will not be null as it must authenticated
			// A new request was made

			// Strings loaded from file
			if(connection != null){
				SocialMediaConnectionProperties oauthConnection = (SocialMediaConnectionProperties)connection;
				
				accessToken = new Token(oauthConnection.getAccessToken(), oauthConnection.getAccessSecret());

				if(serviceProvider.getAuthentication().compareTo("OAUTH") == 0){
					SocialMediaProvider oauthServiceProvider = (SocialMediaProvider)serviceProvider;
					
					String scope = oauthConnection.scopeCSV();
					if(scope != null){
						service = new ServiceBuilder()
						.provider(oauthServiceProvider.getApiClass())
						.apiKey(oauthConnection.getApiKey())
						.apiSecret(oauthConnection.getApiSecret())
						.callback(callbackUrl)
						.scope(scope)
						.build();
					}
					else {
						service = new ServiceBuilder()
						.provider(oauthServiceProvider.getApiClass())
						.apiKey(oauthConnection.getApiKey())
						.apiSecret(oauthConnection.getApiSecret())
						.callback(callbackUrl)
						.build();
					}
				}
			}

			if(service != null){
				service.signRequest(accessToken, (OAuthRequest)request);
			}
		}
		else if(authentication.compareTo("REST") == 0){
			request = new Request(resource.getVerb(), url);
		}
		else {
			request = new Request(resource.getVerb(), url);
		}
		
		try{
			// Process Request Headers
			Map<String, String> headers = resource.getHeaders();
			if(headers != null){
				for(String key : headers.keySet()){
					request.addHeader(key, headers.get(key));
				}
			}
			
			// Process Request Payload
			String payload = resource.getPayload();
			if(payload != null && payload.trim().length() > 0){
				request.addPayload(payload);
			}
			
			logger.info("Executing Request: " + url);
			
			// make a request
			Response response = request.send();

			// record response code
			responseCode = response.getCode();

			// record response body
			responseBody = (response != null) ? response.getBody() : null;
		}catch(Exception e){
			logger.error("Exception occured during HTTP Send: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public int getResponseCode() {
		return responseCode;
	}
	
	public String getResponseBody() {
		return responseBody;
	}

	public void setConnection(ConnectionProperties connection) {
		this.connection = connection;
	}

	public void setOauthService(OAuthService oauthService) {
		this.service = oauthService;
	}

	public void setAccessToken(Token accessToken) {
		this.accessToken = accessToken;
	}
}
