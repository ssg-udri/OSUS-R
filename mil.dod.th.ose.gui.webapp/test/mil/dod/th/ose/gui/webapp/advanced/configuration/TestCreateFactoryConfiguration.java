//==============================================================================
// This software is part of the Open Standard for Unattended Sensors (OSUS)
// reference implementation (OSUS-R).
//
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.
//
// You should have received a copy of the CC0 Public Domain Dedication along
// with this software. If not, see
// <http://creativecommons.org/publicdomain/zero/1.0/>.
//==============================================================================
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationRequestData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import com.google.protobuf.Message;

/**
 * Test class for the {@link CreateFactoryConfiguration} class.
 * 
 * @author bachmakm
 */
public class TestCreateFactoryConfiguration
{
    private CreateFactoryConfigurationImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private List<String> m_Values = new ArrayList<String>();
    private List<String> m_Options = new ArrayList<String>();
    private MessageWrapper m_MessageWrapper;
    
    @Before
    public void setup()
    {      
        m_SUT = new CreateFactoryConfigurationImpl();
        m_MessageFactory = mock(MessageFactory.class);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        m_Values.add("value1");
        m_Values.add("value2");
        m_Values.add("value3");
        
        m_Options.add("option1");
        m_Options.add("option2");
        m_Options.add("option3");
        
        when(m_MessageFactory.createConfigAdminMessage(Mockito.any(ConfigAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
    }    

    /**
     * Verify request to create factory configuration is sent.
     * Verify contents of request match data contained in properties.
     */
    @Test
    public void testCreateConfiguration()
    {    
        //test set up
        UnmodifiableConfigMetatypeModel model = mock(UnmodifiableConfigMetatypeModel.class);
        //create dummy models
        ConfigPropModelImpl configModel1 = ModelFactory.createPropModel("imma key", "imma value");
        ConfigPropModelImpl configModel2 = ModelFactory.createPropModel("key", "value");
        //add models to list
        List<UnmodifiablePropertyModel> propModels = new ArrayList<UnmodifiablePropertyModel>();
        propModels.add(configModel1);
        propModels.add(configModel2);
        when(model.getProperties()).thenReturn(propModels);
        
        m_SUT.setPropertiesList(model, "immaPid", 123);
        
        m_SUT.createConfiguration();
        
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createConfigAdminMessage(eq(ConfigAdminMessageType.
                CreateFactoryConfigurationRequest), captor.capture());
        verify(m_MessageWrapper).queue(eq(123), (ResponseHandler)eq(null));
        
        CreateFactoryConfigurationRequestData data = (CreateFactoryConfigurationRequestData)captor.getValue();
        assertThat(data.getFactoryPid(), is("immaPid"));
        assertThat(data.getFactoryPropertyCount(), is(2));
        assertThat(data.getFactoryProperty(0).getKey(), is("imma key"));
        assertThat(data.getFactoryProperty(1).getKey(), is("key"));
    }    
    
    /**
     * Verify property list can be set.
     * Verify contents of property list are correct. 
     */
    @Test
    public void testGetSetPropertiesList()
    {
        //test set up
        UnmodifiableConfigMetatypeModel model = mock(UnmodifiableConfigMetatypeModel.class);
        //create dummy models
        ConfigPropModelImpl configModel1 = ModelFactory.createPropModel("imma key", "imma value");
        ConfigPropModelImpl configModel2 = ModelFactory.createPropModel("key", "value");

        //add models to list
        List<UnmodifiablePropertyModel> propModels = new ArrayList<UnmodifiablePropertyModel>();
        propModels.add(configModel1);
        propModels.add(configModel2);
        when(model.getProperties()).thenReturn(propModels);
        
        //ensure properties list has not been set
        assertThat(m_SUT.getPropertiesList(), is(nullValue()));
        
        m_SUT.setPropertiesList(model, "pid", 123);
        
        List<ModifiablePropertyModel> propsList = m_SUT.getPropertiesList();
        assertThat(propsList.size(), is(2));
        
        ModifiablePropertyModel retModel1 = propsList.get(0);
        assertThat(retModel1.getName(), is(configModel1.getName()));
        assertThat(retModel1.getKey(), is(configModel1.getKey()));
        assertThat(retModel1.getCardinality(), is(configModel1.getCardinality()));
        assertThat(retModel1.getDescription(), is(configModel1.getDescription()));
        assertThat(retModel1.getValue(), is(configModel1.getValue()));
        
        ModifiablePropertyModel retModel2 = propsList.get(1);
        assertThat(retModel2.getKey(), is(configModel2.getKey()));
        assertThat(retModel2.getName(), is(configModel2.getName()));
        assertThat(retModel2.getValue(), is(configModel2.getValue()));
        assertThat(retModel2.getCardinality(), is(configModel2.getCardinality()));
        assertThat(retModel2.getDescription(), is(configModel2.getDescription()));
    }
}
