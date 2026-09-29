// Writes the signed SAML test fixtures: test-resources/okta-config.xml and
// test-resources/saml/<ID>.b64. Each fixture is a base64 SAML Response in the
// format Okta posts: RSA-SHA256 enveloped signatures with exclusive c14n,
// placed after Issuer, and an unspecified-format NameID.
//
// Usage: java script/GenerateSamlFixtures.java <keysDir> <testResourcesDir>
//
// <keysDir> holds idp.p12 and evil.p12 (aliases idp and evil, password
// changeit). Run it through
// script/generate-saml-fixtures, which makes the keys and deletes them after.
// Needs JDK 17 or later.
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GenerateSamlFixtures {
  static final String PROTOCOL_NS = "urn:oasis:names:tc:SAML:2.0:protocol";
  static final String ASSERTION_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
  static final String IDP_ENTITY_ID = "http://www.okta.com/exk-test";
  static final String SP_ENTITY_ID = "http://localhost:3000/";
  static final String ACS_URL = "http://localhost:3000/login";
  static final String SSO_URL = "https://example.okta.com/app/example_app/exk-test/sso/saml";
  static final String NAME_ID = "Jane.Doe@Example.com";
  static final String SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";
  static final String KEYSTORE_PASSWORD = "changeit";
  static final Instant FAR_FUTURE = Instant.parse("2126-01-01T00:00:00Z");
  static final DateTimeFormatter SAML_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  /** The properties of one Response. The defaults describe V1; each variant changes one. */
  static final class Opts {
    KeyStore.PrivateKeyEntry signer;
    boolean signResponse = true;
    boolean signAssertion = true;
    String issuer = IDP_ENTITY_ID;
    String status = SUCCESS;
    /** Null leaves the Subject with no NameID. */
    String nameId = NAME_ID;
    /** Null leaves Conditions with no AudienceRestriction. */
    String audience = SP_ENTITY_ID;
    String destination = ACS_URL;
    String recipient = ACS_URL;
    Instant notBefore;
    Instant notOnOrAfter = FAR_FUTURE;
    Instant scdNotOnOrAfter = FAR_FUTURE;

    Opts(KeyStore.PrivateKeyEntry signer, Instant now) {
      this.signer = signer;
      notBefore = now.minusSeconds(300);
    }

    Opts(Opts other) {
      signer = other.signer;
      signResponse = other.signResponse;
      signAssertion = other.signAssertion;
      issuer = other.issuer;
      status = other.status;
      nameId = other.nameId;
      audience = other.audience;
      destination = other.destination;
      recipient = other.recipient;
      notBefore = other.notBefore;
      notOnOrAfter = other.notOnOrAfter;
      scdNotOnOrAfter = other.scdNotOnOrAfter;
    }

    Opts with(Consumer<Opts> change) {
      Opts copy = new Opts(this);
      change.accept(copy);
      return copy;
    }
  }

  /** A Response, plus changes made to its signed document or to its final XML text. */
  record Variant(Opts opts, Consumer<Document> afterSigning, UnaryOperator<String> afterSerializing) {
    static Variant of(Opts opts) {
      return new Variant(opts, document -> {}, UnaryOperator.identity());
    }

    Variant afterSigning(Consumer<Document> change) {
      return new Variant(opts, change, afterSerializing);
    }

    Variant afterSerializing(UnaryOperator<String> change) {
      return new Variant(opts, afterSigning, change);
    }
  }

  static String newId() {
    return "id" + UUID.randomUUID().toString().replace("-", "");
  }

  static String assertionXml(String id, Opts o, Instant now) {
    return "<saml2:Assertion xmlns:saml2=\"" + ASSERTION_NS + "\" ID=\"" + id + "\" IssueInstant=\"" + SAML_TIME.format(now) + "\" Version=\"2.0\">"
        + "<saml2:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\">" + o.issuer + "</saml2:Issuer>"
        + "<saml2:Subject>"
        + (o.nameId == null ? "" : "<saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">" + o.nameId + "</saml2:NameID>")
        + "<saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">"
        + "<saml2:SubjectConfirmationData NotOnOrAfter=\"" + SAML_TIME.format(o.scdNotOnOrAfter) + "\" Recipient=\"" + o.recipient + "\"/>"
        + "</saml2:SubjectConfirmation></saml2:Subject>"
        + "<saml2:Conditions NotBefore=\"" + SAML_TIME.format(o.notBefore) + "\" NotOnOrAfter=\"" + SAML_TIME.format(o.notOnOrAfter) + "\">"
        + (o.audience == null ? "" : "<saml2:AudienceRestriction><saml2:Audience>" + o.audience + "</saml2:Audience></saml2:AudienceRestriction>")
        + "</saml2:Conditions>"
        + "<saml2:AuthnStatement AuthnInstant=\"" + SAML_TIME.format(now) + "\" SessionIndex=\"" + id + "\">"
        + "<saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement>"
        + "</saml2:Assertion>";
  }

  static String responseXml(String id, String assertion, Opts o, Instant now) {
    return "<saml2p:Response xmlns:saml2p=\"" + PROTOCOL_NS + "\" Destination=\"" + o.destination + "\" ID=\"" + id + "\" IssueInstant=\"" + SAML_TIME.format(now) + "\" Version=\"2.0\">"
        + "<saml2:Issuer xmlns:saml2=\"" + ASSERTION_NS + "\" Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\">" + o.issuer + "</saml2:Issuer>"
        + "<saml2p:Status><saml2p:StatusCode Value=\"" + o.status + "\"/></saml2p:Status>"
        + assertion
        + "</saml2p:Response>";
  }

  static Document parse(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  static Element firstAssertion(Document document) {
    return (Element) document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
  }

  // Okta puts the enveloped Signature directly after the signed element's Issuer.
  static void sign(Element element, KeyStore.PrivateKeyEntry signer) throws Exception {
    element.setIdAttributeNS(null, "ID", true);
    XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
    String exclusiveC14n = CanonicalizationMethod.EXCLUSIVE;
    Reference reference = factory.newReference("#" + element.getAttribute("ID"),
        factory.newDigestMethod(DigestMethod.SHA256, null),
        List.of(factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                factory.newTransform(exclusiveC14n, (TransformParameterSpec) null)),
        null, null);
    SignedInfo signedInfo = factory.newSignedInfo(
        factory.newCanonicalizationMethod(exclusiveC14n, (C14NMethodParameterSpec) null),
        factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
        List.of(reference));
    KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();
    KeyInfo keyInfo = keyInfoFactory.newKeyInfo(
        List.of(keyInfoFactory.newX509Data(List.of((X509Certificate) signer.getCertificate()))));
    Element issuer = (Element) element.getElementsByTagNameNS(ASSERTION_NS, "Issuer").item(0);
    DOMSignContext context = new DOMSignContext(signer.getPrivateKey(), element, issuer.getNextSibling());
    context.setDefaultNamespacePrefix("ds");
    factory.newXMLSignature(signedInfo, keyInfo).sign(context);
  }

  static String serialize(Document document) throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.INDENT, "no");
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
    return writer.toString();
  }

  static String build(Variant variant, Instant now) throws Exception {
    Opts o = variant.opts();
    Document document = parse(responseXml(newId(), assertionXml(newId(), o, now), o, now));
    if (o.signAssertion) sign(firstAssertion(document), o.signer);
    if (o.signResponse) sign(document.getDocumentElement(), o.signer);
    variant.afterSigning().accept(document);
    return variant.afterSerializing().apply(serialize(document));
  }

  static KeyStore.PrivateKeyEntry loadKey(Path keystore, String alias) throws Exception {
    KeyStore store = KeyStore.getInstance("PKCS12");
    try (InputStream in = Files.newInputStream(keystore)) {
      store.load(in, KEYSTORE_PASSWORD.toCharArray());
    }
    return (KeyStore.PrivateKeyEntry) store.getEntry(alias, new KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray()));
  }

  static String oktaConfig(String certificate) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<configuration>\n"
        + "  <applications>\n"
        + "    <application>\n"
        + "      <md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"" + IDP_ENTITY_ID + "\">\n"
        + "        <md:IDPSSODescriptor WantAuthnRequestsSigned=\"false\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n"
        + "          <md:KeyDescriptor use=\"signing\">\n"
        + "            <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:X509Data><ds:X509Certificate>" + certificate + "</ds:X509Certificate></ds:X509Data></ds:KeyInfo>\n"
        + "          </md:KeyDescriptor>\n"
        + "          <md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</md:NameIDFormat>\n"
        + "          <md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"" + SSO_URL + "\"/>\n"
        + "          <md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"" + SSO_URL + "\"/>\n"
        + "        </md:IDPSSODescriptor>\n"
        + "      </md:EntityDescriptor>\n"
        + "      <sp entityID=\"" + SP_ENTITY_ID + "\" assertionConsumerServiceURL=\"" + ACS_URL + "\"/>\n"
        + "    </application>\n"
        + "  </applications>\n"
        + "</configuration>\n";
  }

  public static void main(String[] args) throws Exception {
    // Okta sends unwrapped base64 in signatures. The JDK signer wraps at 76
    // characters with &#13; unless this property is set.
    System.setProperty("com.sun.org.apache.xml.internal.security.ignoreLineBreaks", "true");
    Path keysDir = Paths.get(args[0]);
    Path testResourcesDir = Paths.get(args[1]);
    Path samlDir = testResourcesDir.resolve("saml");
    Files.createDirectories(samlDir);

    KeyStore.PrivateKeyEntry idp = loadKey(keysDir.resolve("idp.p12"), "idp");
    KeyStore.PrivateKeyEntry evil = loadKey(keysDir.resolve("evil.p12"), "evil");
    Instant now = Instant.now();
    Opts v1 = new Opts(idp, now);
    Opts v2 = v1.with(o -> o.signResponse = false);
    String unsignedEvilAssertion = assertionXml(newId(), v1.with(o -> o.nameId = "attacker@evil.example.com"), now);
    Consumer<Document> insertEvilAssertion = document -> {
      Element signed = firstAssertion(document);
      try {
        signed.getParentNode().insertBefore(document.importNode(parse(unsignedEvilAssertion).getDocumentElement(), true), signed);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };

    Map<String, Variant> variants = new LinkedHashMap<>();
    variants.put("V1", Variant.of(v1));
    variants.put("V2", Variant.of(v2));
    variants.put("N1", Variant.of(v1).afterSigning(document ->
        document.getElementsByTagNameNS(ASSERTION_NS, "NameID").item(0).setTextContent("attacker@evil.example.com")));
    variants.put("N2", Variant.of(v1.with(o -> o.signer = evil)));
    variants.put("N6", Variant.of(v1.with(o -> { o.signResponse = false; o.signAssertion = false; })));
    variants.put("N7a", Variant.of(v2).afterSigning(insertEvilAssertion));
    variants.put("N7b", Variant.of(v1).afterSigning(insertEvilAssertion));
    variants.put("N8", Variant.of(v1.with(o -> o.status = "urn:oasis:names:tc:SAML:2.0:status:Responder")));
    variants.put("N9", Variant.of(v1).afterSerializing(xml ->
        xml.replaceFirst("\\?>", "?><!DOCTYPE saml2p:Response>")));
    variants.put("N10", Variant.of(v1.with(o -> o.issuer = "http://www.okta.com/exk-other")));
    variants.put("N11", Variant.of(v1.with(o -> o.nameId = null)));
    variants.put("N4", Variant.of(v1.with(o -> o.audience = "http://evil.example.com/")));
    variants.put("N4b", Variant.of(v1.with(o -> o.audience = null)));
    variants.put("N5", Variant.of(v1.with(o -> o.destination = "http://evil.example.com/login")));
    variants.put("N5b", Variant.of(v1.with(o -> o.recipient = "http://evil.example.com/login")));
    variants.put("N3", Variant.of(v1.with(o -> {
      o.notBefore = now.minusSeconds(1200);
      o.notOnOrAfter = now.minusSeconds(600);
      o.scdNotOnOrAfter = now.minusSeconds(600);
    })));
    variants.put("N3b", Variant.of(v1.with(o -> o.notBefore = Instant.parse("2125-01-01T00:00:00Z"))));
    variants.put("N3c", Variant.of(v1.with(o -> o.scdNotOnOrAfter = now.minusSeconds(600))));

    for (Map.Entry<String, Variant> entry : variants.entrySet()) {
      String response = build(entry.getValue(), now);
      String base64 = Base64.getEncoder().encodeToString(response.getBytes(StandardCharsets.UTF_8));
      Files.writeString(samlDir.resolve(entry.getKey() + ".b64"), base64 + "\n");
    }
    String certificate = Base64.getEncoder().encodeToString(idp.getCertificate().getEncoded());
    Files.writeString(testResourcesDir.resolve("okta-config.xml"), oktaConfig(certificate));
    System.out.println("Wrote okta-config.xml and " + variants.keySet() + " to " + testResourcesDir);
  }
}
