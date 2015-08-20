///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     Data API
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2015
//
// COPYRIGHT:    University of California, San Francisco, 2015
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.data;

import java.awt.Window;
import java.io.IOException;
import java.util.List;

import mmcorej.TaggedImage;

import org.json.JSONException;

import org.micromanager.internal.utils.MMScriptException;
import org.micromanager.PropertyMap;

/**
 * This class provides general utility functions for working with
 * Micro-Manager data. You can access it via ScriptInterface's data() method
 * (for example, "mm.data().getCoordsBuilder()").
 */
public interface DataManager {
   /**
    * Generate a "blank" CoordsBuilder for use in constructing new Coords
    * instances.
    * @return a CoordsBuilder used to construct new Coords instances.
    */
   public Coords.CoordsBuilder getCoordsBuilder();

   /**
    * Generate a new, "blank" Datastore with RAM-based Storage and return it.
    * This Datastore will not be managed by Micro-Manager by default (see the
    * org.micromanager.api.display.DisplayManager.manage() method for more
    * information).
    * @return an empty RAM-based Datastore.
    */
   public Datastore createRAMDatastore();

   /**
    * Generate a new, "blank" Datastore with multipage TIFF-based Storage and
    * return it. This format stores multiple 2D image planes in the same file,
    * up to 4GB per file. This Datastore will not be managed by Micro-Manager
    * by default (see the org.micromanager.api.display.DisplayManager.manage()
    * method for more information). Be certain to call the freeze() method of
    * the Datastore when you have finished adding data to it, as the Storage
    * must finalize the dataset before it is properly saved to disk.
    *
    * Please note that the multipage TIFF storage system currently only
    * supports the time, Z, channel, and stage position axes for images.
    * Attempts to add images with "custom" axes will create an error dialog,
    * and the image will not be added to the Datastore.
    *
    * @param directory Location on disk to store the file(s).
    * @param shouldGenerateSeparateMetadata if true, a separate metadata.txt
    *        file will be generated.
    * @param shouldSplitPositions if true, then each stage position (per
    *        Coords.STAGE_POSITION) will be in a separate file.
    * @return an empty Datastore backed by disk in the form of one or more
    *         TIFF files each containing multiple image planes.
    * @throws IOException if any errors occur while opening files for writing.
    */
   public Datastore createMultipageTIFFDatastore(String directory,
         boolean shouldGenerateSeparateMetadata, boolean shouldSplitPositions)
         throws IOException;

   /**
    * Generate a new, "blank" Datastore whose Storage is a series of
    * single-plane TIFF files. This Datastore will not be managed by
    * Micro-Manager by default (see the
    * org.micromanager.api.display.DisplayManager.manage() method for more
    * information).  Be certain to call the freeze() method of the Datastore
    * when you have finished adding data to it, as the Storage must finalize
    * the dataset before it is properly saved to disk.
    *
    * Please note that the single-plane TIFF series storage system currently
    * only supports the time, Z, channel, and stage position axes for images.
    * Attempts to add images with "custom" axes will create an error dialog,
    * and the image will not be added to the Datastore.
    *
    * @param directory Location on disk to store the files.
    * @return an empty Datastore backed by disk in the form of multiple
    *         single-plane TIFF files.
    */
   public Datastore createSinglePlaneTIFFSeriesDatastore(String directory);

   /**
    * Display a dialog prompting the user to select a location on disk, and
    * invoke loadData() on that selection. If you then want that data to be
    * displayed in an image display window, use the
    * DisplayManager.createDisplay() method or the
    * DisplayManager.loadDisplays() method.
    * @param parent Window to show the dialog on top of. May be null.
    * @param isVirtual if true, then only the data required will be loaded from
    *        disk; otherwise the entire dataset will be loaded. See note on
    *        loadData.
    * @return The Datastore generated by loadData(), or null if the user
    *         cancels the dialog.
    * @throws IOException if loadData() encountered difficulties.
    */
   public Datastore promptForDataToLoad(Window parent, boolean isVirtual) throws IOException;

   /**
    * Load the image data at the specified location on disk, and return a
    * Datastore for that data. This Datastore will not be managed by
    * Micro-Manager by default (see the
    * org.micromanager.api.display.DisplayManager.manage() method for more
    * information).
    * @param directory Location on disk from which to pull image data.
    * @param isVirtual If true, then only load images into RAM as they are
    *        requested. This reduces RAM utilization and has a lesser delay
    *        at the start of image viewing, but has worse performance if the
    *        entire dataset needs to be viewed or manipulated.
    * @return A Datastore backed by appropriate Storage that provides access
    *        to the images.
    * @throws IOException if there was any error in reading the data.
    */
   public Datastore loadData(String directory, boolean isVirtual) throws IOException;

   /**
    * Generate a new Image with the provided pixel data, rules for interpreting
    * that pixel data, coordinates, and metadata.
    * @param pixels A byte[] or short[] array of unsigned pixel data.
    * @param width Width of the image, in pixels
    * @param height Height of the image, in pixels
    * @param bytesPerPixel How many bytes are allocated to each pixel in the
    *        image. Currently only 1, 2, and 4-byte images are supported.
    * @param numComponents How many colors are encoded into each pixel.
    *        Currently only 1-component (grayscale) and 3-component (RGB)
    *        images are supported.
    * @param coords Coordinates defining the position of the Image within a
    *        larger dataset.
    * @param metadata Image metadata.
    * @return A new Image based on the input parameters.
    */
   public Image createImage(Object pixels, int width, int height,
         int bytesPerPixel, int numComponents, Coords coords,
         Metadata metadata);

   /**
    * Given a TaggedImage input, output an Image based on the TaggedImage.
    * Note that certain properties in the metadata in the TaggedImage's tags
    * will be converted into the scopeData and userData PropertyMaps of the
    * Metadata, based on the assumption that the TaggedImage was generated by
    * the hardware that this instance of Micro-Manager is controlling. If you
    * do not want this to happen, you will need to call the version of
    * convertTaggedImage() that takes a Coords and Metadata parameter.
    * @param tagged TaggedImage to be converted
    * @return An Image based on the TaggedImage
    * @throws JSONException if the TaggedImage's metadata cannot be read
    * @throws MMScriptException if portions of the TaggedImage's metadata are
    *         malformed.
    */
   public Image convertTaggedImage(TaggedImage tagged) throws JSONException, MMScriptException;

   /**
    * Given a TaggedImage input, output an Image based on the TaggedImage,
    * but with the Coords and/or Metadata optionally overridden.
    * @param tagged TaggedImage to be converted
    * @param coords Coords at which the new image is located. If null, then
    *        the coordinate information in the TaggedImage will be used.
    * @param metadata Metadata for the new image. If null, then the metadata
    *        will be derived from the TaggedImage instead.
    * @return An Image based on the TaggedImage
    * @throws JSONException if the TaggedImage's metadata cannot be read
    * @throws MMScriptException if portions of the TaggedImage's metadata are
    *         malformed.
    */
   public Image convertTaggedImage(TaggedImage tagged, Coords coords,
         Metadata metadata) throws JSONException, MMScriptException;

   /**
    * Generate a "blank" MetadataBuilder for use in constructing new
    * Metadata instances.
    * @return a MetadataBuilder for creating new Metadata instances.
    */
   public Metadata.MetadataBuilder getMetadataBuilder();

   /**
    * Generate a "blank" SummaryMetadataBuilder for use in constructing new
    * SummaryMetadata instances.
    * @return a SummaryMetadataBuilder for creating new SummaryMetadata
    *         instances.
    */
   public SummaryMetadata.SummaryMetadataBuilder getSummaryMetadataBuilder();

   /**
    * Generate a "blank" PropertyMap.Builder with all null values.
    * @return a PropertyMapBuilder for creating new PropertyMap instances.
    */
   public PropertyMap.PropertyMapBuilder getPropertyMapBuilder();

   /**
    * Create a new Pipeline using the provided list of ProcessorFactories.
    * @param factories List of ProcessorFactories which will each be used, in
    *        order, to create a Processor for the new Pipeline.
    * @param store Datastore in which Images should be stored after making
    *        their way through the Pipeline.
    * @param isSynchronous If true, then every call to Pipeline.insertImage()
    *        will block until the input Image has been "fully consumed" by
    *        the pipeline (any result Image(s) have been added to the Datastore
    *        the Pipeline is connected to). If false, Pipeline.insertImage()
    *        will return immediately and the Images will arrive in the
    *        Datastore at some indeterminate later time.
    * @return a Pipeline containing Processors as specified by the input
    *         factories.
    */
   public Pipeline createPipeline(List<ProcessorFactory> factories,
         Datastore store, boolean isSynchronous);

   /**
    * Create a copy of the current application Pipeline as configured in the
    * "Data Processing Pipeline" window.
    * @return a Pipeline based on the current GUI pipeline.
    */
   public Pipeline copyApplicationPipeline();
}