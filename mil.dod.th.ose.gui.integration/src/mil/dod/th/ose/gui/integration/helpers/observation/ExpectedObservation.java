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
package mil.dod.th.ose.gui.integration.helpers.observation;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.detection.TargetClassificationTypeEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;

/**
 * Object which represents the expected observation that is to be displayed.
 * @author nickmarcucci
 *
 */
public class ExpectedObservation
{
    /**
     * The name of the asset that produced the observation.
     */
    private String m_AssetName; 
    
    /**
     * The sensor id that the observation is labeled with.
     */
    private String m_SensorId;
    
    /**
     * The operating mode of the controller that produced this observation.
     */
    private OperationMode m_OpMode;
    
    /**
     * The type of the observation.
     */
    private ObservationSubTypeEnum m_Type;
    
    /**
     * The target classifications for this observation.
     */
    private List<TargetClassificationTypeEnum> m_TgtClasses;
    
    /**
     * The sensing modalities for this observation.
     */
    private List<SensingModalityEnum> m_Modalities;
    
    /**
     * The observation data that is displayed in the expanded content.
     */
    private List<String> m_ObservationData;
    
    /**
     * If the observation is a digital media type, then the mime type
     * is the mime type of the observation.
     */
    private String m_MimeType;
    
    /**
     * Indicates if this observation holds a detection.
     */
    private boolean m_IsObsDetection;
    
    /**
     * The related observation structure that holds the observation's
     * related observations.
     */
    private RelatedObservation m_RelatedObs;
    
    /**
     * The general information that should appear with the observation.
     */
    private GeneralObservationInformation m_GeneralInfo;
    
    /**
     * Contains the observed time if set in the observation.
     */
    private Long m_ObservedTime;

    /**
     * Constructor.
     * @param assetName
     *  the asset name of the observation
     * @param type
     *  the observation type
     * @param mode
     *  the operation mode of the observation
     * @param isDetection
     *  whether or not the observation contains a detection
     */
    public ExpectedObservation(String assetName, ObservationSubTypeEnum type, OperationMode mode, boolean isDetection)
    {
        m_AssetName = assetName;
        m_Type = type;
        m_OpMode = mode;
        
        m_IsObsDetection = isDetection;
        
        m_TgtClasses = new ArrayList<>();
        m_Modalities = new ArrayList<>();
        m_ObservationData = new ArrayList<>();
        
        m_GeneralInfo = null;
        m_ObservedTime = null;
    }
    
    /**
     * Sets the sensor id for the observation.
     * @param id
     *  the sensor id
     * @return
     *  the observation object
     */
    public ExpectedObservation withSensorId(String id)
    {
        m_SensorId = id;
        return this;
    }
    
    /**
     * Sets the target classifications for the observation. The first entry
     * is expected to be the entry that is shown in the header of the observation.
     * @param classifications
     *  the target classifications 
     * @return
     *  the observation object
     */
    public ExpectedObservation withTargetClassifications(TargetClassificationTypeEnum ... classifications)
    {
        for (TargetClassificationTypeEnum tgts : classifications)
        {
            m_TgtClasses.add(tgts);
        }
        
        return this;
    }
    
    /**
     * Sets the sensing modalities for the observation. The first entry is expected to be
     * the entry that is shown in the header of the observation.
     * @param modalities
     *  the sensing modalities
     * @return
     *  the observation object
     */
    public ExpectedObservation withSensingModalities(SensingModalityEnum ... modalities)
    {
        for (SensingModalityEnum modality : modalities)
        {
            m_Modalities.add(modality);
        }
        
        return this;
    }
    
    /**
     * Sets the expected observation data that is to be displayed.
     * @param data
     *  the observation data
     * @return
     *  the observation object
     */
    public ExpectedObservation withObservationData(String ... data)
    {
        for (String datum : data)
        {
            m_ObservationData.add(datum);
        }
        
        return this;
    }
    
    /**
     * Sets the related observations for the observation.
     * @param related
     *  the object which contains all relations for the observation
     * @return
     *  the observation object
     */
    public ExpectedObservation withRelatedObservation(RelatedObservation related)
    {
        m_RelatedObs = related;
        
        return this;
    }
    
    /**
     * Sets the mime type for the observation.
     * @param type
     *  the mime type
     * @return
     *  the observation object
     */
    public ExpectedObservation withMimeType(String type)
    {
        m_MimeType = type;
        return this;
    }
    
    /**
     * Sets the expected general information that should appear with the observation.
     * @param info
     *  the general information that should appear for the observation
     * @return
     *  the observation object
     */
    public ExpectedObservation withGeneralInformation(GeneralObservationInformation info)
    {
        m_GeneralInfo = info;
        return this;
    }
    
    /**
     * Sets the observed time for the observation.
     * 
     * @param observedTime
     *  observed time for the observation
     * @return
     *  the observation object
     */
    public ExpectedObservation withObservedTime(Long observedTime)
    {
        m_ObservedTime = observedTime;
        return this;
    }

    /**
     * Retrieves the asset name.
     * @return
     *  the asset name
     */
    public String getAssetName()
    {
        return m_AssetName;
    }
    
    /**
     * Retrieves the mime type.
     * @return
     *  the mime type.
     */
    public String getMimeType()
    {
        return m_MimeType;
    }
    
    /**
     * Retrieves the sensor id.
     * @return
     *  the sensor id
     */
    public String getSensorId()
    {
        return m_SensorId;
    }
    
    /**
     * Retrieves the observation sub-type of the observation.
     * @return
     *  the observation's type
     */
    public ObservationSubTypeEnum getObservationSubTypeEnum()
    {
        return m_Type;
    }
    
    /**
     * Retrieves the system mode of the observation.
     * @return
     *  the system mode
     */
    public OperationMode getSystemMode()
    {
        return m_OpMode;
    }
    
    /**
     * Retrieves the observation data expected
     * @return
     *  the observation data 
     */
    public List<String> getObsData()
    {
        return m_ObservationData;
    }
    
    /**
     * Retrieves the image name for the set observation type.
     * @return
     *  the image for the set observation type.
     */
    public String retrieveObservationTypeEnumImage()
    {
        return ObservationImageHelper.getObservationSubTypeEnumImage(m_Type);
    }
    
    /**
     * Retrieves the related observation structure.
     * @return
     *  the related observation object
     */
    public RelatedObservation getRelatedObservation()
    {
        return m_RelatedObs;
    }
    
    /**
     * Retrieves the general information object for this observation.
     * @return
     *  the general information object
     */
    public GeneralObservationInformation getGeneralInfo()
    {
        return m_GeneralInfo;
    }
    
    /**
     * Function which returns a list of image names that are expected in the 
     * observation header. (Function expects that header is comprised of the observation
     * type image, the first target classification image, the first sensing modality image,
     * a detection image if the observation is a detection, and a link image if the observation
     * contains related observations).
     * @return
     *  the list of images.
     */
    public List<String> retrieveExpectedHeaderImages()
    {
        List<String> images = new ArrayList<>();
        
        if (m_Type != null)
        {
            images.add(ObservationImageHelper.getObservationSubTypeEnumImage(m_Type));
        
            if (m_IsObsDetection && !m_Type.equals(ObservationSubTypeEnum.DETECTION))
            {
                images.add(ObservationImageHelper.getObservationSubTypeEnumImage(ObservationSubTypeEnum.DETECTION));
            }
        }
        
        if (m_RelatedObs != null)
        {
            images.add(ObservationConstants.IMG_LINK);
        }
        
        return images;
    }
    
    /**
     * Retrieves the list of expected target classification images.
     * @return
     *  the image names of all the target classifications that will
     *  be displayed.
     */
    public List<String> retrieveExpectedClassImages()
    {
        List<String> images = new ArrayList<>();
        
        for (TargetClassificationTypeEnum tgt : m_TgtClasses)
        {
            images.add(ObservationImageHelper.getTargetClassificationImage(tgt));
        }
        
        return images;
    }
    
    /**
     * Retrieves the list of image names that are to be displayed for this observation.
     * @return
     *  the image names of all the sensing modality images that are to be shown
     */
    public List<String> retrieveExpectedModalityImages()
    {
        List<String> images = new ArrayList<>();
        
        for (SensingModalityEnum modality : m_Modalities)
        {
            images.add(ObservationImageHelper.getSensingModalityImage(modality));
        }
        
        return images;
    }
    
    /**
     * Retrieves the system mode image name based on the observation's set system mode.
     * @return
     *  the image name of the system mode icon that is shown with the observation
     */
    public String retireveExpectedSystemModeImage()
    {
        String image = "";
        switch (m_OpMode)
        {
            case OPERATIONAL_MODE:
                image = ObservationConstants.IMG_OP_MODE;
                break;
            case TEST_MODE:
                image = ObservationConstants.IMG_TEST_MODE;
                break;
        }
        
        return image;
    }
    
    /**
     * Function to retrieve the expected image that is displayed when this observation 
     * is displayed as a related observation.
     * @return
     *  the image name of the displayed observation
     */
    public String retrieveRelatedObservationImage()
    {
        if (m_IsObsDetection)
        {
            return ObservationImageHelper.getObservationSubTypeEnumImage(ObservationSubTypeEnum.DETECTION);
        }
        
        return retrieveObservationTypeEnumImage();
    }
    
    /**
     * Indicates whether the observation is a digital media type based on the 
     * set observation type.
     * @return
     *  true if observation is audio, image, or video type.
     */
    public boolean isDigitalMedia()
    {
        if (m_Type.equals(ObservationSubTypeEnum.AUDIO_METADATA) || m_Type.equals(ObservationSubTypeEnum.IMAGE_METADATA)
                || m_Type.equals(ObservationSubTypeEnum.VIDEO_METADATA))
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Indicates if the observation contains a detection.
     * @return
     *  true if observation contains a detection.
     */
    public boolean isDetection()
    {
        return m_IsObsDetection;
    }
    
    /**
     * Indicates if the observation has related observations.
     * @return
     *  true if the observation has related observations; false otherwise
     */
    public boolean hasRelatedObs()
    {
        if (m_RelatedObs == null)
        {
            return false;
        }
        
        return true;
    }

    /**
     * Indicates if the observation has the observed time stamp.
     * 
     * @return
     *  true if the observation has observed time stamp; false otherwise
     */
    public boolean hasObservedTime()
    {
        if (m_ObservedTime == null)
        {
            return false;
        }
 
        return true;
    }

    /**
     * Indicates if the observation has sensing modalities.
     * 
     * @return
     *      true if observation has sensings, false otherwise
     */
    public boolean hasSensingModalities()
    {
        if (m_Modalities.isEmpty())
        {
            return false;
        }

        return true;
    }

    /**
     * Returns the heading based on the set observation type.
     * 
     * @return
     *  the string heading that matches the observation type
     */
    public String getObservationHeading()
    {
        String heading = "";
        
        switch (m_Type)
        {
            case WEATHER:
                heading = "Weather";
                break;
            case IMAGE_METADATA:
                heading = "Image";
                break;
            case AUDIO_METADATA:
                heading = "Audio";
                break;
            case VIDEO_METADATA:
                heading = "Video";
                break;
            case STATUS:
                heading = "Status";
                break;
            case DETECTION:
                heading = "Detection";
                break;
            case BIOLOGICAL:
                heading = "Biological";
                break;
            case CBRNE_TRIGGER:
                heading = "CBRNE Trigger";
                break;
            case CHEMICAL:
                heading = "Chemical";
                break;
            case WATER_QUALITY:
                heading = "Water Quality";
                break;
            case POWER:
                heading = "Power";
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown observation type %s", m_Type));
        }
        
        return heading;
    }
}
