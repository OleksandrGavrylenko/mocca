/*
 * Copyright 2008 Federal Chancellery Austria and
 * Graz University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.gv.egiz.stal.service.translator;

/**
 * 
 * @author Clemens Orthacker <clemens.orthacker@iaik.tugraz.at>
 */
public class TranslationException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  private Class<?> unknownClass;

  /**
   * Creates a new instance of <code>TranslationException</code> without detail message.
   * @param unknownClass the class that could not be translated
   */
  public TranslationException(Class<?> unkownClass) {
    this.unknownClass = unkownClass;
  }

  @Override
  public String getMessage() {
    return "Failed to translate type " + unknownClass;
  }



}