GlassWhatIsThat
===============

My first attempt at a google glass app for the search by image technique like the others. It couples Glass well.

Right now the system will take a single snapshot when you say "ok glass, what is that". From this image it will display the image and the screen will remain on, while this is occuring it will send the original image as well any subimage (by seeing any significant edged object) to Google in order to get the results which will be placed on the timeline with a tagline on each of the images with the result that Google provided. 

Best bet to get a result is to hold up a DVD / Book infront of the Glass at arms length in order to get a result.



*Note* In order to compile this, it requires OpenCV Library - 2.4.8 and it needs to be linked to the project in the project.properties with a reference like :

android.library.reference.1=../OpenCV-2.4.8-android-sdk/sdk/java

