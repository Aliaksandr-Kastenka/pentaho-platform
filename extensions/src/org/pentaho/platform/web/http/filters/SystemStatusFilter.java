/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2015 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.platform.web.http.filters;

import org.apache.commons.lang.StringUtils;
import org.pentaho.platform.api.engine.IServerStatusProvider;
import org.pentaho.platform.engine.core.system.status.ServerStatusProvider;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * The purpose of this filter is to check to make sure that the platform is properly initialized before letting requests
 * in.
 * 
 */
public class SystemStatusFilter extends ForwardFilter {

  public void init( final FilterConfig filterConfig ) throws ServletException {
    String failurePage = filterConfig.getInitParameter( "initFailurePage" ); //$NON-NLS-1$
    failurePage = StringUtils.defaultIfBlank( failurePage, "InitFailure" ); //$NON-NLS-1$
    setRedirectPath( "/" + failurePage ); //$NON-NLS-1$ 
  }
  
  @Override
  protected boolean isEnable() {
    return IServerStatusProvider.ServerStatus.ERROR == ServerStatusProvider.getInstance().getStatus();
  }

  public void destroy() {
    // nothing
  }

}
