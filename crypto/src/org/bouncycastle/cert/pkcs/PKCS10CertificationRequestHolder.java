package org.bouncycastle.cert.pkcs;

import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.cert.CertException;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;

public class PKCS10CertificationRequestHolder
{
    private CertificationRequest certificationRequest;

    private static CertificationRequest parseBytes(byte[] encoding)
        throws IOException
    {
        try
        {
            return CertificationRequest.getInstance(ASN1Object.fromByteArray(encoding));
        }
        catch (ClassCastException e)
        {
            throw new CertIOException("malformed data: " + e.getMessage(), e);
        }
        catch (IllegalArgumentException e)
        {
            throw new CertIOException("malformed data: " + e.getMessage(), e);
        }
    }
    
    public PKCS10CertificationRequestHolder(CertificationRequest certificationRequest)
    {
         this.certificationRequest = certificationRequest;
    }

    /**
     * Create a PKCS10CertificationRequestHolder from the passed in bytes.
     *
     * @param encoded BER/DER encoding of the CertificationRequest structure.
     * @throws IOException in the event of corrupted data, or an incorrect structure.
     */
    public PKCS10CertificationRequestHolder(byte[] encoded)
        throws IOException
    {
        this(parseBytes(encoded));
    }

    public CertificationRequest toASN1Structure()
    {
         return certificationRequest;
    }

    public byte[] getEncoded()
        throws IOException
    {
        return certificationRequest.getEncoded();
    }

    public boolean isSignatureValid(ContentVerifierProvider verifierProvider)
        throws CertException
    {
        CertificationRequestInfo requestInfo = certificationRequest.getCertificationRequestInfo();

        ContentVerifier verifier;

        try
        {
            verifier = verifierProvider.get(certificationRequest.getSignatureAlgorithm());

            OutputStream sOut = verifier.getOutputStream();

            sOut.write(requestInfo.getDEREncoded());

            sOut.close();
        }
        catch (Exception e)
        {
            throw new CertException("unable to process signature: " + e.getMessage(), e);
        }

        return verifier.verify(certificationRequest.getSignature().getBytes());
    }
}