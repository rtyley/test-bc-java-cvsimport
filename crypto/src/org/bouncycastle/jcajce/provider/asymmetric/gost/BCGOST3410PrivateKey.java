package org.bouncycastle.jcajce.provider.asymmetric.gost;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.cryptopro.GOST3410PublicKeyAlgParameters;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.params.GOST3410PrivateKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import org.bouncycastle.jce.interfaces.GOST3410Params;
import org.bouncycastle.jce.interfaces.GOST3410PrivateKey;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.jce.spec.GOST3410ParameterSpec;
import org.bouncycastle.jce.spec.GOST3410PrivateKeySpec;
import org.bouncycastle.jce.spec.GOST3410PublicKeyParameterSetSpec;

public class BCGOST3410PrivateKey
    implements GOST3410PrivateKey, PKCS12BagAttributeCarrier
{
    private BigInteger          x;

    private transient   GOST3410Params      gost3410Spec;
    private transient   PKCS12BagAttributeCarrier attrCarrier = new PKCS12BagAttributeCarrierImpl();

    protected BCGOST3410PrivateKey()
    {
    }

    BCGOST3410PrivateKey(
        GOST3410PrivateKey key)
    {
        this.x = key.getX();
        this.gost3410Spec = key.getParameters();
    }

    BCGOST3410PrivateKey(
        GOST3410PrivateKeySpec spec)
    {
        this.x = spec.getX();
        this.gost3410Spec = new GOST3410ParameterSpec(new GOST3410PublicKeyParameterSetSpec(spec.getP(), spec.getQ(), spec.getA()));
    }

    BCGOST3410PrivateKey(
        PrivateKeyInfo info)
        throws IOException
    {
        GOST3410PublicKeyAlgParameters    params = new GOST3410PublicKeyAlgParameters((ASN1Sequence)info.getAlgorithmId().getParameters());
        ASN1OctetString      derX = ASN1OctetString.getInstance(info.parsePrivateKey());
        byte[]              keyEnc = derX.getOctets();
        byte[]              keyBytes = new byte[keyEnc.length];
        
        for (int i = 0; i != keyEnc.length; i++)
        {
            keyBytes[i] = keyEnc[keyEnc.length - 1 - i]; // was little endian
        }
        
        this.x = new BigInteger(1, keyBytes);
        this.gost3410Spec = GOST3410ParameterSpec.fromPublicKeyAlg(params);
    }

    BCGOST3410PrivateKey(
        GOST3410PrivateKeyParameters params,
        GOST3410ParameterSpec spec)
    {
        this.x = params.getX();
        this.gost3410Spec = spec;

        if (spec == null) 
        {
            throw new IllegalArgumentException("spec is null");
        }
    }

    public String getAlgorithm()
    {
        return "GOST3410";
    }

    /**
     * return the encoding format we produce in getEncoded().
     *
     * @return the string "PKCS#8"
     */
    public String getFormat()
    {
        return "PKCS#8";
    }

    /**
     * Return a PKCS8 representation of the key. The sequence returned
     * represents a full PrivateKeyInfo object.
     *
     * @return a PKCS8 representation of the key.
     */
    public byte[] getEncoded()
    {
        PrivateKeyInfo          info;
        byte[]                  keyEnc = this.getX().toByteArray();
        byte[]                  keyBytes;

        if (keyEnc[0] == 0)
        {
            keyBytes = new byte[keyEnc.length - 1];
        }
        else
        {
            keyBytes = new byte[keyEnc.length];
        }
        
        for (int i = 0; i != keyBytes.length; i++)
        {
            keyBytes[i] = keyEnc[keyEnc.length - 1 - i]; // must be little endian
        }

        try
        {
            if (gost3410Spec instanceof GOST3410ParameterSpec)
            {
                info = new PrivateKeyInfo(new AlgorithmIdentifier(CryptoProObjectIdentifiers.gostR3410_94, new GOST3410PublicKeyAlgParameters(new ASN1ObjectIdentifier(gost3410Spec.getPublicKeyParamSetOID()), new ASN1ObjectIdentifier(gost3410Spec.getDigestParamSetOID()))), new DEROctetString(keyBytes));
            }
            else
            {
                info = new PrivateKeyInfo(new AlgorithmIdentifier(CryptoProObjectIdentifiers.gostR3410_94), new DEROctetString(keyBytes));
            }

            return info.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public GOST3410Params getParameters()
    {
        return gost3410Spec;
    }

    public BigInteger getX()
    {
        return x;
    }

    public void setBagAttribute(
        ASN1ObjectIdentifier oid,
        ASN1Encodable        attribute)
    {
        attrCarrier.setBagAttribute(oid, attribute);
    }

    public ASN1Encodable getBagAttribute(
        DERObjectIdentifier oid)
    {
        return attrCarrier.getBagAttribute(oid);
    }

    public Enumeration getBagAttributeKeys()
    {
        return attrCarrier.getBagAttributeKeys();
    }

    private void readObject(
        ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        String publicKeyParamSetOID = (String)in.readObject();
        if (publicKeyParamSetOID != null)
        {
            this.gost3410Spec = new GOST3410ParameterSpec(publicKeyParamSetOID, (String)in.readObject(), (String)in.readObject());
        }
        else
        {
            this.gost3410Spec = new GOST3410ParameterSpec(new GOST3410PublicKeyParameterSetSpec((BigInteger)in.readObject(), (BigInteger)in.readObject(), (BigInteger)in.readObject()));
            in.readObject();
            in.readObject();
        }
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
    }

    private void writeObject(
        ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        if (gost3410Spec.getPublicKeyParamSetOID() != null)
        {
            out.writeObject(gost3410Spec.getPublicKeyParamSetOID());
            out.writeObject(gost3410Spec.getDigestParamSetOID());
            out.writeObject(gost3410Spec.getEncryptionParamSetOID());
        }
        else
        {
            out.writeObject(null);
            out.writeObject(gost3410Spec.getPublicKeyParameters().getP());
            out.writeObject(gost3410Spec.getPublicKeyParameters().getQ());
            out.writeObject(gost3410Spec.getPublicKeyParameters().getA());
            out.writeObject(gost3410Spec.getDigestParamSetOID());
            out.writeObject(gost3410Spec.getEncryptionParamSetOID());
        }
    }
}
