/*
 * Copyright (c) 2017 CEA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    CEA - initial API and implementation
 */
package org.eclipse.sensinact.gateway.nthbnd.rest.internal.ws;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.eclipse.sensinact.gateway.core.method.Parameter;
import org.eclipse.sensinact.gateway.core.security.Authentication;
import org.eclipse.sensinact.gateway.core.security.AuthenticationToken;
import org.eclipse.sensinact.gateway.core.security.Credentials;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccessWrapper;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundMediator;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundRecipient;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundRequest;

public class WsRestAccessRequest implements NorthboundAccessWrapper 
{
	private NorthboundMediator mediator;
	private JSONObject request;
	private Authentication<?> authentication;
	private String content;
	private WebSocketWrapper socket;

	public WsRestAccessRequest(NorthboundMediator mediator, 
			WebSocketWrapper socket, JSONObject request)
	{
		this.request = request;
		this.mediator = mediator;
		this.socket = socket;
	}
	
	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccessWrapper#getMediator()
	 */
	@Override
	public NorthboundMediator getMediator() 
	{
		return this.mediator;
	}
	
	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccessWrapper#getRequestID(org.eclipse.sensinact.gateway.core.method.Parameter[])
	 */
	@Override
	public String getRequestID(Parameter[] parameters) 
	{	
		String rid = (String) request.opt("rid");
		if(rid == null)
		{
			List<String> list = getQueryMap().get("rid");
			if(list!=null && list.size()>0)
			{
				rid = list.get(0);
			}
		}
		return rid;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccessWrapper#getRequestURI()
	 */
	@Override
	public String getRequestURI() 
	{
		String uri = request.optString("uri");
		String[] uriElements = uri.split("\\?");
		return uriElements[0];
	}
	
	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccessWrapper#getQueryMap()
	 */
	@Override
	public Map<String,List<String>> getQueryMap() 
	{
		String uri = request.optString("uri");
		String[] uriElements = uri.split("\\?");
		if(uriElements.length == 2)
		{
			try
			{
				return NorthboundRequest.processRequestQuery(
						uriElements[1]);
			}
			catch (UnsupportedEncodingException e)
			{
				this.mediator.error(e);
			}
		}
		return Collections.<String,List<String>>emptyMap();
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccessWrapper#getContent()
	 */
	@Override
	public String getContent() 
	{
		if(this.content == null)
		{
			JSONArray parameters =  request.optJSONArray(
					"parameters");
			if(parameters == null)
			{
				parameters = new JSONArray();
			}
			this.content = parameters.toString();
		}
		return this.content;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccessWrapper#getAuthentication()
	 */
	@Override
	public Authentication<?> getAuthentication() 
	{
		if(this.authentication == null)
		{
			String tokenHeader = (String) request.opt("token");
			String login = (String) request.opt("login");
			String password = (String) request.opt("password");
			
			if(tokenHeader != null)
			{
				this.authentication = new AuthenticationToken(
						tokenHeader);
		
			} else if(login != null && password != null)
			{
				this.authentication =  new Credentials(
						login, password);
			}
		}
		return this.authentication;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccessWrapper#createRecipient(org.eclipse.sensinact.gateway.core.method.Parameter[])
	 */
	@Override
	public NorthboundRecipient createRecipient(Parameter[] parameters)
	{
		return new WebSocketRecipient(mediator, socket);
	}
}
