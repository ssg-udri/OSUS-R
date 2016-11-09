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
package edu.udayton.udri.asset.novatel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests NovatelStatusService functionality.
 * @author nickmarcucci
 *
 */
public class TestNovatelStatusService
{
    private NovatelStatusService m_SUT;
    private List<ComponentTypeEnum> m_ComponentTypes;
    private List<String> m_Descriptors;
    private ComponentTypeEnum m_TimeServiceIdentifier = ComponentTypeEnum.SOFTWARE_UNIT;
    private ComponentTypeEnum m_MessageReaderIdentifier = ComponentTypeEnum.NAVIGATION;
    private String m_TimeDesc = "Time Service";
    private String m_MessageReaderDesc = "INS Message Reader";
    
    @Before
    public void setUp()
    {
        m_SUT = new NovatelStatusService();
        
        m_ComponentTypes = Arrays.asList(m_MessageReaderIdentifier, m_TimeServiceIdentifier);
        m_Descriptors = Arrays.asList(m_MessageReaderDesc, m_TimeDesc);
    }
    
    /**
     * Verify that if an overall status has not yet been 
     * created then an overall status will be made and will contain the 
     * appropriate fields.
     */
    @Test
    public void testGetStatusOverallStatusNotKnown()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.GOOD, SummaryStatusEnum.GOOD);
        List<ComponentStatus> compStatus = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(compStatus);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
        assertThat(status.getSummaryStatus().getDescription(), is("Novatel asset is processing data as expected."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, compStatus);
    }
    
    /**
     * Verify that if previous status and the component statuses passed in haven't changed then a 
     * new status isn't made.
     */
    @Test
    public void testGetStatusStatusNoChange()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.GOOD, SummaryStatusEnum.GOOD);
        List<ComponentStatus> lists = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(lists);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
        assertThat(status.getSummaryStatus().getDescription(), is("Novatel asset is processing data as expected."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, lists);
        
        Observation observation = new Observation();
        observation.setStatus(status);
        
        Status newStatus = m_SUT.getStatus(observation, lists.get(0));
        assertThat(newStatus, nullValue());
    }
    
    /**
     * Verify that if a status changes after the original has been made then a new status is made and returned.
     */
    @Test
    public void testGetStatusStatusHasChanged()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.GOOD, SummaryStatusEnum.GOOD);
        List<ComponentStatus> lists = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses); 
        Status status = getLastStatus(lists);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
        assertThat(status.getSummaryStatus().getDescription(), is("Novatel asset is processing data as expected."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, lists);
        
        Observation observation = new Observation();
        observation.setStatus(status);
        
        statuses = Arrays.asList(SummaryStatusEnum.DEGRADED, SummaryStatusEnum.GOOD);
        
        lists = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        
        Status newStatus = m_SUT.getStatus(observation, lists.get(0));
        assertThat(newStatus.getSummaryStatus().getSummary(), is(SummaryStatusEnum.DEGRADED));
        assertThat(newStatus.getSummaryStatus().getDescription(), 
                is("Not all supporting components are functioning as expected."));
        
        verifyStatuses(newStatus.getComponentStatuses(), lists);
    }
    
    /**
     * Verify that if one component status is bad then overall status is bad.
     */
    @Test
    public void testGetStatusStatusReturnedBad()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.BAD, SummaryStatusEnum.GOOD);
        List<ComponentStatus> list = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(list);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.BAD));
        assertThat(status.getSummaryStatus().getDescription(), is(
                "One or more supporting components are not functioning properly."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, list);
    }
    
    /**
     * Verify that if one status is degraded and one is good then status should be degraded.
     */
    @Test
    public void testGetStatusDegraded()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.DEGRADED, SummaryStatusEnum.GOOD);
        List<ComponentStatus> list = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(list);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.DEGRADED));
        assertThat(status.getSummaryStatus().getDescription(), 
                is("Not all supporting components are functioning as expected."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, list);
    }
    
    /**
     * Verify that if all components are off then overall status is off.
     */
    @Test
    public void testGetStatusStatusOff()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.OFF, SummaryStatusEnum.OFF);
        List<ComponentStatus> list = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(list);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.OFF));
        assertThat(status.getSummaryStatus().getDescription(), is("Novatel asset is not actively processing data."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, list);
    }
    
    /**
     * Verify that if all components are good then overall status is good.
     */
    @Test
    public void testGetStatusStatusGood()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.GOOD, SummaryStatusEnum.GOOD);
        List<ComponentStatus> list = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(list);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
        assertThat(status.getSummaryStatus().getDescription(), is("Novatel asset is processing data as expected."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, list);
    }
    
    /**
     * Verify that if any component status is unknown then overall status is unknown.
     */
    @Test
    public void testGetStatusStatusUnknown()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.GOOD, SummaryStatusEnum.UNKNOWN);
        List<ComponentStatus> list = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(list);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.UNKNOWN));
        assertThat(status.getSummaryStatus().getDescription(), is("Not all components have a known status."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, list);
    }
    
    /**
     * Verify that if time service is off, that the INS status is used for the overall status.
     */
    @Test
    public void testGetStatusStatusGoodTimeOff()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.GOOD, SummaryStatusEnum.OFF);
        List<ComponentStatus> list = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(list);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
        assertThat(status.getSummaryStatus().getDescription(), is("Novatel asset is processing data as expected."));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, list);
    }
    
    /**
     * Verify that if a new component status is received with a new component type that a new status is created.
     */
    @Test
    public void testNewComponent()
    {
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.GOOD, SummaryStatusEnum.GOOD);
        List<ComponentStatus> list = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(list);
        
        assertThat(status, notNullValue());
        assertThat(status.getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
       
        List<ComponentStatus> compStatuses = status.getComponentStatuses();
        verifyStatuses(compStatuses, list);
        
        Observation observation = new Observation();
        observation.setStatus(status);
        
        //new status component
        ComponentType type = new ComponentType(ComponentTypeEnum.POWER, "I got the POWAR!");
        Status newStatus = m_SUT.getStatus(observation, new ComponentStatus(
                type, new OperatingStatus(SummaryStatusEnum.OFF, "I'm off")));
        assertThat(newStatus, is(notNullValue()));
        assertThat(newStatus.getComponentStatuses().size(), is(3));
        
        //organize results
        Map<ComponentTypeEnum, SummaryStatusEnum> resultMap = new HashMap<>();
        for (ComponentStatus statusComp : newStatus.getComponentStatuses())
        {
            resultMap.put(statusComp.getComponent().getType(), statusComp.getStatus().getSummary());
        }
        //verify
        assertThat(resultMap.get(ComponentTypeEnum.NAVIGATION), is(SummaryStatusEnum.GOOD));
        assertThat(resultMap.get(ComponentTypeEnum.SOFTWARE_UNIT), is(SummaryStatusEnum.GOOD));
        assertThat(resultMap.get(ComponentTypeEnum.POWER), is(SummaryStatusEnum.OFF));
        assertThat(newStatus.getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
    }
    
    /**
     * Verify that if a component status description changes, but the summary is NOT different
     * that the status is updated.
     */
    @Test
    public void testSameCompStatusDiffDescription()
    {
        //new status component
        List<SummaryStatusEnum> statuses = Arrays.asList(SummaryStatusEnum.GOOD, SummaryStatusEnum.GOOD);
        m_ComponentTypes = Arrays.asList(m_MessageReaderIdentifier, m_TimeServiceIdentifier);
        m_Descriptors = Arrays.asList(m_MessageReaderDesc, m_TimeDesc);
        List<ComponentStatus> list = getMockComponentStatuses(m_ComponentTypes, m_Descriptors, statuses);
        Status status = getLastStatus(list);
        Observation observation = new Observation();
        observation.setStatus(status);

        //change a description
        ComponentType componentType = new ComponentType(m_MessageReaderIdentifier, m_MessageReaderDesc);
        Status newStatus = m_SUT.getStatus(observation, 
                new ComponentStatus(componentType, new OperatingStatus(SummaryStatusEnum.GOOD, "I am different")));
        
        assertThat(newStatus, is(notNullValue()));
        
        //inspect
        Map<ComponentTypeEnum, String> typeWithSumDescription = new HashMap<>();
        for (ComponentStatus statusFromList : newStatus.getComponentStatuses())
        {
            typeWithSumDescription.put(statusFromList.getComponent().getType(), 
                    statusFromList.getStatus().getDescription());
        }
        assertThat(typeWithSumDescription.get(ComponentTypeEnum.NAVIGATION), is("I am different"));
    }
    
    /**
     * Function to verify that the component statuses match the given map of component statuses.
     * @param compStatuses
     *  the list of generated component statuses from the overall created status object
     * @param expectedCompStatList
     *  the map of generated component statuses
     */
    private void verifyStatuses(List<ComponentStatus> compStatuses, List<ComponentStatus> expectedCompStatList)
    {
        assertThat(compStatuses, notNullValue());
        assertThat(compStatuses.size(), is(expectedCompStatList.size()));
        
        Map<ComponentTypeEnum, ComponentStatus> expectedStatusesMap = new HashMap<>();
        for (ComponentStatus expectedStatus : expectedCompStatList)
        {
            expectedStatusesMap.put(expectedStatus.getComponent().getType(), expectedStatus);
        }
        for (ComponentTypeEnum expectedStatus : expectedStatusesMap.keySet())
        {
            boolean found = false; 
            ComponentStatus status = expectedStatusesMap.get(expectedStatus);
            
            for (ComponentStatus newStatuses : compStatuses)
            {
                assertThat(newStatuses, notNullValue());
                if (newStatuses.getComponent().getDescription()
                        .equals(status.getComponent().getDescription()))
                {
                    found = true;
                    assertThat(newStatuses.getComponent().getType(), is(status.getComponent().getType()));
                    assertThat(newStatuses.getStatus().getSummary(), is(status.getStatus().getSummary()));
                    assertThat(newStatuses.getStatus().getDescription(), is(status.getStatus().getDescription()));
                    break;
                }
            }
            
            assertThat(String.format("Looking for component status: " 
                    + "Type: %s Description: %s but it was not found in the status returned.", 
                    status.getComponent().getType(), status.getComponent().getDescription()),found, is(true));
        }
    }
    
    /**
     * Function to produce a map which contains the generated component types as keys and the overall component
     * status based on the given input lists. Function expects that all lists are the same size.
     * @param types
     *  the types of the components that are to be added
     * @param componentDescriptors
     *  the description that is to be assigned to the component type
     * @param statuses
     *  the statuses that each of the components should have
     * @return
     *  the list of the created component statuses
     */
    private List<ComponentStatus> getMockComponentStatuses(List<ComponentTypeEnum> types, 
            List<String> componentDescriptors, List<SummaryStatusEnum> statuses)
    {
        if (types.size() != componentDescriptors.size() || types.size() != statuses.size())
        {
            throw new IllegalStateException(String.format("Lists for creating component map are not the same size. " 
                    + "ComponenTypes: %d ComponentDescriptors: %d Statuses: %d", 
                    types.size(), componentDescriptors.size(), statuses.size()));
        }
        
        List<ComponentStatus> list = new ArrayList<>();
        for (int i = 0; i < types.size(); i++)
        {
            final ComponentType compType = new ComponentType();
            final ComponentStatus status = new ComponentStatus();
            final OperatingStatus opStatus = new OperatingStatus();
            
            compType.setType(types.get(i));
            compType.setDescription(componentDescriptors.get(i));
            status.setComponent(compType);
            
            opStatus.setSummary(statuses.get(i));
            opStatus.setDescription(String.format("%s: Some stat description", componentDescriptors.get(i)));
            
            status.setStatus(opStatus);
            
            list.add(status);
        }
        return list;
    }
    
    /**
     * Get the overall status after submitting a list of component statuses.
     * @param statuses
     *      the list of statuses to submit
     */
    private Status getLastStatus(List<ComponentStatus> statuses)
    {
        for (int i = 0; i < statuses.size() -1; i++)
        {
            m_SUT.getStatus(null, statuses.get(i));
        }
        return m_SUT.getStatus(null, statuses.get(statuses.size() - 1));
    }
}
