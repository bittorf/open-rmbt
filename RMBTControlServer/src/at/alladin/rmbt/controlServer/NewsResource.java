/*******************************************************************************
 * Copyright 2013 alladin-IT OG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.alladin.rmbt.controlServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public class NewsResource extends ServerResource
{
    @Post("json")
    public String request(final String entity)
    {
        addAllowOrigin();
        
        JSONObject request = null;
        
        final ErrorList errorList = new ErrorList();
        final JSONObject answer = new JSONObject();
        String answerString;
        
        System.out.println(MessageFormat.format(labels.getString("NEW_NEWS"), getIP()));
        
        if (entity != null && !entity.isEmpty())
            // try parse the string to a JSON object
            try
            {
                request = new JSONObject(entity);
                
                String lang = request.optString("language");
                
                // Load Language Files for Client
                
                final List<String> langs = Arrays.asList(settings.getString("RMBT_SUPPORTED_LANGUAGES").split(",\\s*"));
                
                if (langs.contains(lang))
                {
                    errorList.setLanguage(lang);
                    labels = (PropertyResourceBundle) ResourceBundle.getBundle("at.alladin.rmbt.res.SystemMessages",
                            new Locale(lang));
                }
                else
                    lang = settings.getString("RMBT_DEFAULT_LANGUAGE");
                
//                System.out.println(request.toString(4));
                
                if (conn != null)
                {
                    final long lastNewsUid = request.optLong("lastNewsUid");
                    final String plattform = request.optString("plattform");
                    final int softwareVersionCode = request.optInt("softwareVersionCode", -1);
                    
                    final JSONArray newsList = new JSONArray();
                    
                    if (softwareVersionCode != -1) // no news for old (buggy) versions
                    {
                        try
                        {
                            
                            final PreparedStatement st = conn
                                    .prepareStatement("SELECT uid,title_" + lang + 
                                            " AS title, text_" + lang +
                                            " AS text FROM news " +
                                            " WHERE" +
                                            " (uid > ? OR force = true)" +
                                            " AND active = true" +
                                            " AND (plattform IS NULL OR plattform = ?)" +
                                            " AND (? <= max_software_version_code)" +
                                            " ORDER BY time ASC");
                            st.setLong(1, lastNewsUid);
                            st.setString(2, plattform);
                            st.setInt(3, softwareVersionCode);
                            
                            final ResultSet rs = st.executeQuery();
                            
                            while (rs.next())
                            {
                                final JSONObject jsonItem = new JSONObject();
                                
                                jsonItem.put("uid", rs.getInt("uid"));
                                jsonItem.put("title", rs.getString("title"));
                                jsonItem.put("text", rs.getString("text"));
                                
                                newsList.put(jsonItem);
                            }
                            
                            rs.close();
                            st.close();
                        }
                        catch (final SQLException e)
                        {
                            e.printStackTrace();
                            errorList.addError("ERROR_DB_GET_NEWS_SQL");
                        }
                    }
                    
                    answer.put("news", newsList);
                    
                }
                else
                    errorList.addError("ERROR_DB_CONNECTION");
                
            }
            catch (final JSONException e)
            {
                errorList.addError("ERROR_REQUEST_JSON");
                System.out.println("Error parsing JSON Data " + e.toString());
            }
        else
            errorList.addErrorString("Expected request is missing.");
        
        try
        {
            answer.putOpt("error", errorList.getList());
        }
        catch (final JSONException e)
        {
            System.out.println("Error saving ErrorList: " + e.toString());
        }
        
        answerString = answer.toString();
        
        return answerString;
    }
    
    @Get("json")
    public String retrieve(final String entity)
    {
        return request(entity);
    }
    
}
