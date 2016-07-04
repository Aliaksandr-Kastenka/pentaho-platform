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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.mantle.client.commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.filechooser.RepositoryFile;
import org.pentaho.gwt.widgets.client.utils.NameUtils;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleOutputLocationDialog;
import org.pentaho.mantle.client.events.SolutionFileHandler;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.solutionbrowser.SolutionBrowserPanel;
import org.pentaho.mantle.client.solutionbrowser.filelist.FileItem;

public class RunInMyBackgroundCommand extends AbstractCommand {

  String moduleBaseURL = GWT.getModuleBaseURL();
  String moduleName = GWT.getModuleName();
  String contextURL = moduleBaseURL.substring( 0, moduleBaseURL.lastIndexOf( moduleName ) );
  private FileItem repositoryFile;

  public RunInMyBackgroundCommand() {
  }

  private String solutionPath = null;
  private String outputLocationPath = null;
  private String outputName = null;
  private String jobId = null;

  public String getSolutionPath() {
    return solutionPath;
  }

  public void setJobId( String jobId ) {
    this.jobId = jobId;
  }

  public String getJobId() {
    return jobId;
  }

  public void setSolutionPath( String solutionPath ) {
    this.solutionPath = solutionPath;
  }

  public String getOutputLocationPath() {
    return outputLocationPath;
  }

  public void setOutputLocationPath( String outputLocationPath ) {
    this.outputLocationPath = outputLocationPath;
  }

  public String getModuleBaseURL() {
    return moduleBaseURL;
  }

  public void setModuleBaseURL( String moduleBaseURL ) {
    this.moduleBaseURL = moduleBaseURL;
  }

  public String getOutputName() {
    return outputName;
  }

  public void setOutputName( String outputName ) {
    this.outputName = outputName;
  }

  protected void performOperation() {
    final SolutionBrowserPanel sbp = SolutionBrowserPanel.getInstance();
    if ( this.getSolutionPath() != null ) {
      sbp.getFile( this.getSolutionPath(), new SolutionFileHandler() {
        @Override
        public void handle( RepositoryFile repositoryFile ) {
          RunInMyBackgroundCommand.this.repositoryFile = new FileItem( repositoryFile, null, null, false, null );
          showDialog( true );
        }
      } );
    } else {
      performOperation( true );
    }
  }

  protected void showDialog( final boolean feedback ) {
    final ScheduleOutputLocationDialog outputLocationDialog = new ScheduleOutputLocationDialog( solutionPath ) {
      @Override
      protected void onSelect( final String name, final String outputLocationPath ) {
        setOutputName( name );
        setOutputLocationPath( outputLocationPath );
        performOperation( feedback );
      }
    };

    outputLocationDialog.setOkButtonText( Messages.getString( "ok" ) );
    outputLocationDialog.center();	
  }

  private RequestBuilder createTreeRequest() {
    RequestBuilder scheduleFileRequestBuilder = new RequestBuilder( RequestBuilder.GET, contextURL + "api/repo/files/"
        + NameUtils.encodeRepositoryPath( outputLocationPath ) + "/tree?depth=1" );
    scheduleFileRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    return scheduleFileRequestBuilder;
  }

  protected void performOperation( boolean feedback ) {

    RequestBuilder treeRequestBuilder = createTreeRequest();

    try {
      treeRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
              new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
          dialogBox.center();
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            String folderId = null;
            Document repository = XMLParser.parse(response.getText());
            NodeList fileNodeList = repository.getElementsByTagName("file");
            for (int i = 0; i < fileNodeList.getLength(); i++) { 
              Element element = (Element)fileNodeList.item(i);
              Node pathNode = element.getElementsByTagName("path").item(0);			  

              if( outputLocationPath.equals( pathNode.getFirstChild().getNodeValue() ) ) {
                folderId = element.getElementsByTagName("id").item(0).getFirstChild().getNodeValue();
              }	
            }

            final JSONObject scheduleRequest = new JSONObject();
            scheduleRequest.put( "folderId", new JSONString( folderId ) ); //$NON-NLS-1$
            scheduleRequest.put( "newName", new JSONString( outputName ) ); //$NON-NLS-1$

            RequestBuilder scheduleFileRequestBuilder = new RequestBuilder( RequestBuilder.POST, contextURL + "plugin/reporting/api/jobs/"
                + jobId + "/schedule/location?folderId=" + folderId + "&newName=" +outputName ); //$NON-NLS-1$
            scheduleFileRequestBuilder.setHeader( "Content-Type", "application/json" ); //$NON-NLS-1$//$NON-NLS-2$
            scheduleFileRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );

            try {
              scheduleFileRequestBuilder.sendRequest( "", new RequestCallback() {

                @Override
                public void onError( Request request, Throwable exception ) {
                  MessageDialogBox dialogBox =
                      new MessageDialogBox(
                          Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
                      dialogBox.center();
                }

                @Override
                public void onResponseReceived( Request request, Response response ) {
                  if ( response.getStatusCode() == 200 ) {
                              
                  } else {
                    MessageDialogBox dialogBox =
                        new MessageDialogBox(
                            Messages.getString( "error" ), Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-2$ //$NON-NLS-3$
                            false, false, true );
                    dialogBox.center();
                  }
                }

              } );
            } catch ( RequestException e ) {
              MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "error" ), e.toString(), //$NON-NLS-1$
                  false, false, true );
              dialogBox.center();
            }
          } else {
            MessageDialogBox dialogBox =
                new MessageDialogBox(
                    Messages.getString( "error" ), Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode(), false, false, true ); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            dialogBox.center();
          }
        }

      } );
    } catch ( RequestException e ) {
      MessageDialogBox dialogBox =
          new MessageDialogBox( Messages.getString( "error" ), e.toString(), false, false, true ); //$NON-NLS-1$
      dialogBox.center();
    }
  }  
}
