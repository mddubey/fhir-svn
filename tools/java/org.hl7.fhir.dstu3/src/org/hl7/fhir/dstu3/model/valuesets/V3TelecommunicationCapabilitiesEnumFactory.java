package org.hl7.fhir.dstu3.model.valuesets;

/*
  Copyright (c) 2011+, HL7, Inc.
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without modification, 
  are permitted provided that the following conditions are met:
  
   * Redistributions of source code must retain the above copyright notice, this 
     list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright notice, 
     this list of conditions and the following disclaimer in the documentation 
     and/or other materials provided with the distribution.
   * Neither the name of HL7 nor the names of its contributors may be used to 
     endorse or promote products derived from this software without specific 
     prior written permission.
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
  POSSIBILITY OF SUCH DAMAGE.
  
*/

// Generated on Wed, Feb 24, 2016 10:46+1100 for FHIR v1.3.0


import org.hl7.fhir.dstu3.model.EnumFactory;

public class V3TelecommunicationCapabilitiesEnumFactory implements EnumFactory<V3TelecommunicationCapabilities> {

  public V3TelecommunicationCapabilities fromCode(String codeString) throws IllegalArgumentException {
    if (codeString == null || "".equals(codeString))
      return null;
    if ("data".equals(codeString))
      return V3TelecommunicationCapabilities.DATA;
    if ("fax".equals(codeString))
      return V3TelecommunicationCapabilities.FAX;
    if ("sms".equals(codeString))
      return V3TelecommunicationCapabilities.SMS;
    if ("tty".equals(codeString))
      return V3TelecommunicationCapabilities.TTY;
    if ("voice".equals(codeString))
      return V3TelecommunicationCapabilities.VOICE;
    throw new IllegalArgumentException("Unknown V3TelecommunicationCapabilities code '"+codeString+"'");
  }

  public String toCode(V3TelecommunicationCapabilities code) {
    if (code == V3TelecommunicationCapabilities.DATA)
      return "data";
    if (code == V3TelecommunicationCapabilities.FAX)
      return "fax";
    if (code == V3TelecommunicationCapabilities.SMS)
      return "sms";
    if (code == V3TelecommunicationCapabilities.TTY)
      return "tty";
    if (code == V3TelecommunicationCapabilities.VOICE)
      return "voice";
    return "?";
  }

    public String toSystem(V3TelecommunicationCapabilities code) {
      return code.getSystem();
      }

}
