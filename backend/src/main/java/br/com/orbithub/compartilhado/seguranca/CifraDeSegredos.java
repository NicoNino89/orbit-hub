package br.com.orbithub.compartilhado.seguranca;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Cifra de segredos guardados no banco
 * Objetivo: Cifrar e decifrar segredos que a aplicação precisa ler de volta (o segredo do segundo fator), para
 *           que um vazamento do banco não entregue os segundos fatores da equipe.
 * Decisões: AES-256-GCM, com vetor de 12 bytes novo a cada cifra e marca de autenticação de 128 bits (detecta
 *           adulteração). A chave é derivada da chave secreta da aplicação (a mesma origem do JWT, que na nuvem
 *           vem do Key Vault), com um rótulo próprio, então nunca fica no banco. O texto leva o prefixo "v1:",
 *           para permitir trocar o esquema no futuro. Trocar a chave da aplicação invalida os segredos: nesse
 *           caso, a equipe configura o segundo fator de novo (procedimento no README).
 */
@Component
public class CifraDeSegredos {

    private static final String PREFIXO = "v1:";
    private static final String ROTULO = "orbit-hub/cifra-de-segredos/v1";
    private static final int BYTES_DO_VETOR = 12;
    private static final int BITS_DA_MARCA = 128;

    private final SecretKeySpec chave;
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Criação da cifra
     * Objetivo: Derivar a chave AES a partir da chave secreta da aplicação.
     * Decisões: SHA-256 do rótulo com a chave; 32 bytes, AES-256.
     */
    public CifraDeSegredos(SecretKey chaveDaAplicacao) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(ROTULO.getBytes(StandardCharsets.UTF_8));
            sha.update(chaveDaAplicacao.getEncoded());
            this.chave = new SecretKeySpec(sha.digest(), "AES");
        } catch (GeneralSecurityException impossivel) {
            throw new IllegalStateException("SHA-256 indisponível", impossivel);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Cifrar
     * Objetivo: Cifrar um texto para guardar no banco.
     * Decisões: Vetor aleatório no começo do resultado; tudo em Base64.
     */
    public String cifrar(String texto) {
        try {
            byte[] vetor = new byte[BYTES_DO_VETOR];
            aleatorio.nextBytes(vetor);
            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(BITS_DA_MARCA, vetor));
            byte[] cifrado = cifra.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] tudo = ByteBuffer.allocate(vetor.length + cifrado.length).put(vetor).put(cifrado).array();
            return PREFIXO + Base64.getEncoder().encodeToString(tudo);
        } catch (GeneralSecurityException falha) {
            throw new IllegalStateException("Falha ao cifrar", falha);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Decifrar
     * Objetivo: Recuperar o texto guardado.
     * Decisões: Texto adulterado, de outra chave ou sem o prefixo é recusado com IllegalStateException.
     */
    public String decifrar(String guardado) {
        if (guardado == null || !guardado.startsWith(PREFIXO)) {
            throw new IllegalStateException("Segredo em formato desconhecido");
        }
        try {
            byte[] tudo = Base64.getDecoder().decode(guardado.substring(PREFIXO.length()));
            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(BITS_DA_MARCA, tudo, 0, BYTES_DO_VETOR));
            byte[] aberto = cifra.doFinal(tudo, BYTES_DO_VETOR, tudo.length - BYTES_DO_VETOR);
            return new String(aberto, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException falha) {
            throw new IllegalStateException("Não foi possível decifrar o segredo", falha);
        }
    }
}
