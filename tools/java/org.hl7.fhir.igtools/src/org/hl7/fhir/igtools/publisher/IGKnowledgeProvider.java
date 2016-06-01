package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.metamodel.ParserBase;
import org.hl7.fhir.dstu3.metamodel.Property;
import org.hl7.fhir.dstu3.model.BaseConformance;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueType;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.dstu3.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.utils.IWorkerContext;
import org.hl7.fhir.dstu3.utils.ProfileUtilities.ProfileKnowledgeProvider;
import org.hl7.fhir.dstu3.utils.ProfileUtilities.ProfileKnowledgeProvider.BindingResolution;
import org.hl7.fhir.dstu3.validation.ValidationMessage;
import org.hl7.fhir.dstu3.validation.ValidationMessage.Source;
import org.hl7.fhir.igtools.renderers.ValidationPresenter.ValidationOutcomes;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class IGKnowledgeProvider implements ProfileKnowledgeProvider, ParserBase.ILinkResolver {

  private IWorkerContext context;
  private JsonObject specPaths;
  private Map<String, JsonObject> igPaths = new HashMap<String, JsonObject>();
  private Set<String> msgs = new HashSet<String>();
  private String pathToSpec;
  private String canonical;
  private ValidationOutcomes errors;
  
  public IGKnowledgeProvider(IWorkerContext context, String pathToSpec, JsonObject igs, ValidationOutcomes errors) throws Exception {
    super();
    this.context = context;
    this.pathToSpec = pathToSpec;
    this.errors = errors;
    loadPaths(igs);
  }

  private void loadPaths(JsonObject igs) throws Exception {
    JsonElement e = igs.get("canonicalBase");
    if (e == null)
      throw new Exception("You must define a canonicalBase in the json file");
    canonical = e.getAsString();
    JsonArray a = igs.getAsJsonArray("paths");
    if (a == null)
      throw new Exception("You must provide paths in the json file");
    for (JsonElement i : a) {
      if (!(i instanceof JsonObject))
        throw new Exception("Unexpected type in paths - must be an object");
      JsonObject o = (JsonObject) i;
      JsonElement p = o.get("url");
      if (p == null)
        throw new Exception("You must provide a URL on each path in the json file");
      if (igPaths.containsKey(p.getAsString()))
        throw new Exception("Duplicated URL "+p.getAsString()+" on path in the json file");
      igPaths.put(p.getAsString(), o);
      p = o.get("base");
      if (p == null)
        throw new Exception("You must provide a base on each path in the json file");
      if (!(p instanceof JsonPrimitive))
        throw new Exception("Unexpected type in paths - base must be an primitive");
      p = o.get("defns");
      if (p != null && !(p instanceof JsonPrimitive))
        throw new Exception("Unexpected type in paths - defns must be an primitive");
    }
  }

  public void loadSpecPaths(JsonObject paths) {
    this.specPaths = paths;
    for (BaseConformance bc : context.allConformanceResources()) {
      JsonElement e = paths.get(bc.getUrl());
      if (e != null)
        bc.setUserData("path", specPath(e.getAsString()));
    }    
  }

  public void checkForPath(BaseConformance bc) {
    if (!bc.getUrl().endsWith("/"+bc.getId()))
      error("Resource id/url mismatch: "+bc.getId()+"/"+bc.getUrl());
    JsonObject e = igPaths.get(bc.getUrl());
    if (e == null)
      e = igPaths.get(bc.getId());
    if (e == null)
      e = igPaths.get(bc.getResourceType().toString()+"/"+bc.getId());
    if (e == null)
      error("No Paths for Resource: "+bc.getUrl());
    else
      bc.setUserData("path", e.get("base").getAsString());
  }

  private void error(String msg) {
    if (!msgs.contains(msg)) {
      msgs.add(msg);
      errors.getErrors().add(new ValidationMessage(Source.Publisher, IssueType.INVARIANT, msg, IssueSeverity.ERROR));
    }
  }

  private String makeCanonical(String ref) {
    return canonical+"/"+ref;
  }

  private void brokenLinkWarning(String ref) {
    String s = "The reference "+ref+" could not be resolved";
    if (!msgs.contains(s)) {
      msgs.add(s);
      errors.getErrors().add(new ValidationMessage(Source.Publisher, IssueType.INVARIANT, s, IssueSeverity.ERROR));
    }
  }

  private String specPath(String path) {
    return pathToSpec+"/"+path;
  }

  public String getDefinitions(StructureDefinition sd) {
    JsonObject e = igPaths.get(sd.getUrl());
    if (e == null)
      e = igPaths.get(sd.getId());
    if (e == null)
      e = igPaths.get(sd.getResourceType().toString()+"/"+sd.getId());
    if (e == null)
      error("No Paths for Resource: "+sd.getUrl());
    else {
      JsonElement p = e.get("defns");
      if (p == null)
        error("No Definition Path for Resource: "+sd.getUrl());
      return p.getAsString();
    }
    return "??";
  }

  // ---- overrides ---------------------------------------------------------------------------
  
  @Override
  public boolean isDatatype(String name) {
    StructureDefinition sd = context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/"+name);
    return sd != null && (sd.getKind() == StructureDefinitionKind.PRIMITIVETYPE || sd.getKind() == StructureDefinitionKind.COMPLEXTYPE) && sd.getDerivation() == TypeDerivationRule.SPECIALIZATION;
  }  

  @Override
  public boolean isResource(String name) {
    StructureDefinition sd = context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/"+name);
    return sd != null && (sd.getKind() == StructureDefinitionKind.RESOURCE) && sd.getDerivation() == TypeDerivationRule.SPECIALIZATION;
  }

  @Override
  public boolean hasLinkFor(String name) {
    return isDatatype(name) || isResource(name);
  }

  @Override
  public String getLinkFor(String corepath, String name) {
    StructureDefinition sd = context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/"+name);
    if (sd != null && sd.hasUserData("path"))
        return sd.getUserString("path");
    brokenLinkWarning(name);
    return name+".html";
  }

  @Override
  public BindingResolution resolveBinding(ElementDefinitionBindingComponent binding) {
    BindingResolution br = new BindingResolution();
    if (!binding.hasValueSet()) {
      br.url = specPath("terminologies.html#unbound");
      br.display = "(unbound)";      
    } else if (binding.getValueSet() instanceof UriType) {
      String ref = ((UriType) binding.getValueSet()).getValue();
      if (ref.startsWith("http://hl7.org/fhir/ValueSet/v3-")) {
        br.url = specPath("v3/"+ref.substring(26)+"/index.html");
        br.display = ref.substring(26);
      } else {
        ValueSet vs = context.fetchResource(ValueSet.class, ref);
        if (vs != null) {
          br.url = vs.getUserString("path");
          br.display = vs.getName();
        } else {
          br.url = ref;
          if (ref.equals("http://tools.ietf.org/html/bcp47"))
            br.display = "IETF BCP-47";
          else if (ref.equals("http://www.rfc-editor.org/bcp/bcp13.txt"))
            br.display = "IETF BCP-13";
          else if (ref.equals("http://www.ncbi.nlm.nih.gov/nuccore?db=nuccore"))
            br.display = "NucCore";
          else if (ref.equals("https://rtmms.nist.gov/rtmms/index.htm#!rosetta"))
            br.display = "Rosetta";
          else if (ref.equals("http://www.iso.org/iso/country_codes.htm"))
            br.display = "ISO Country Codes";
          else
            br.display = "????";
        }
      }
    } else {
      String ref = ((Reference) binding.getValueSet()).getReference();
      if (ref.startsWith("ValueSet/")) {
        ValueSet vs = context.fetchResource(ValueSet.class, makeCanonical(ref));
        if (vs == null) {
          br.url = ref.substring(9)+".html"; // broken link, 
          br.display = ref.substring(9);
          brokenLinkWarning(ref);
        } else {
          br.url = vs.getUserString("path");
          br.display = vs.getName(); 
        }
      } else { 
        if (ref.startsWith("http://hl7.org/fhir/ValueSet/")) {
          ValueSet vs = context.fetchResource(ValueSet.class, ref);
          if (vs != null) { 
            br.url = vs.getUserString("path");
            br.display = vs.getName(); 
          } else { 
            br.display = ref.substring(29);
            br.url = ref.substring(29)+".html";
            brokenLinkWarning(ref);
          }
        } else if (ref.startsWith("http://hl7.org/fhir/ValueSet/v3-")) {
          br.url = specPath("v3/"+ref.substring(26)+"/index.html"); 
          br.display = ref.substring(26);
        } else if (ref.startsWith("http://hl7.org/fhir/ValueSet/v2-")) {
          br.url = specPath("v2/"+ref.substring(26)+"/index.html"); 
          br.display = ref.substring(26);
        } else {
          brokenLinkWarning(ref);
          br.url = ref;
          br.display = "????";
        }
      }
    }
    return br;
  }

  @Override
  public String getLinkForProfile(StructureDefinition profile, String url) {
    StructureDefinition sd = context.fetchResource(StructureDefinition.class, url);
    if (sd != null && sd.hasUserData("path"))
        return sd.getUserString("path")+"|"+sd.getName();
    brokenLinkWarning(url);
    return "unknown.html|??";
  }


  @Override
  public String resolveType(String type) {
    return getLinkFor("", type);
  }

  @Override
  public String resolveProperty(Property property) {
    String path = property.getDefinition().getPath();
    return property.getStructure().getUserString("path")+"#"+path;
  }

  @Override
  public String resolvePage(String name) {
    return specPath(name);
  }

  @Override
  public boolean prependLinks() {
    return false;
  }

  
}