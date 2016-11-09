==============================================================================
 This software is part of the Open Standard for Unattended Sensors (OSUS)
 reference implementation (OSUS-R).

 To the extent possible under law, the author(s) have dedicated all copyright
 and related and neighboring rights to this software to the public domain
 worldwide. This software is distributed without any warranty.

 You should have received a copy of the CC0 Public Domain Dedication along
 with this software. If not, see
 <http://creativecommons.org/publicdomain/zero/1.0/>.
==============================================================================

Notes on Terra Harvest Themes

- For a complete guide on how to create a new theme that can be used by the 
  Terra Harvest Web GUI, see the Primefaces 3.4 Users Guide Section 7.2 Creating
  a New Theme. The guide can be retrieved from http://www.primefaces.org/documentation.html

Note: The accepted file structure for any custom theme that is to be added to the GUI must 
      follow the file structure outlined in the Primefaces 3.4 User Guide. The theme structure
      is as follows:
      
      - META-INF
        - resources
            - primefaces-<yourthemenamehere>
                - theme.css
                - images

1. Modifying Day/Night Themes to incorporate into the Terra Harvest Web GUI
    - Use jQuery Theme Roller (http://jqueryui.com/themeroller/) and configure all the selections
      to the colors that are desired.
      
    - Select Download theme and make sure that Components Toggle All checkbox is unselected.
    
    - From the downloaded zip, navigate to the css folder -> <name of created theme> and copy the 
      images folder and the desired css file and copy into the corresponding day or night theme folders
      in this project.
    
    - Rename css file (should be named something like jquery-ui-....custom.css) to theme.css
      
    - All image references in theme.css must be changed to the following format using
      'primefaces-yourtheme'. An example is below:
        
        Originally:
            url("images/ui-bg_highlight-hard_100_f9f9f9_1x100.png");
        
        Should be changed to:
            url("#{resource['primefaces-<yourtheme>:images/ui-bg_highlight-hard_100_f9f9f9_1x100.png']}")
        
      For modifications for the day theme <yourtheme> needs to be TH-Day
      For modifications for the night theme <yourtheme> needs to be TH-Night
    
    - The GUI will automatically build the jars and incorporate them into the GUI.
    
2. Additional Themes
    
    - Follow the steps above.
    
    - Additional Themes will require modifications to Web GUI code. Modifications include
      altering the theme drop down on the mainscreenTemplate.xhtml and the ThemeManagerBean
      if the current default selection needs to be changed.
      
3. Modifications to image files
    - JQuery theme roller provides multiple image files in different colors. Image files for different colors
      can be identified by the hex color code included in the image file name. Typically, the hex color code
      appears between the identifier name and the image size. 
      
      Ex. ui-bg_highlight-hard_100_f9f9f9_1x100.png
        - f9f9f9 = is the hex color code
        
      Since there could be multiple ui-bg_highlight-hard_100_<hexcolor>_1x100.png make sure that any changes 
      to one file are duplicated in the other color images of the same type.