package br.com.orbithub.identidade.service;

import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.compartilhado.seguranca.SegurancaProperties;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Emissão do access token
 * Objetivo: Gerar o JWT curto que autentica cada requisição.
 * Decisões: Claims mínimas: sub (id da conta), sid (sessão), papeis, termos_pendentes (KAN-92), iss, iat,
 *           exp e jti. Nada de e-mail ou nome no token, para não espalhar dado pessoal em logs e ferramentas.
 */
@Service
public class TokenAcessoService {

    private final JwtEncoder codificador;
    private final SegurancaProperties propriedades;
    private final Clock relogio;
    private final AceiteService aceites;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Criação do serviço
     * Objetivo: Receber o assinador, os parâmetros e o relógio.
     * Decisões: Relógio injetado para os testes controlarem o tempo.
     */
    public TokenAcessoService(JwtEncoder codificador, SegurancaProperties propriedades, Clock relogio,
                              AceiteService aceites) {
        this.codificador = codificador;
        this.propriedades = propriedades;
        this.relogio = relogio;
        this.aceites = aceites;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Gerar access token
     * Objetivo: Assinar um access token para a conta, ligado à sessão informada.
     * Decisões: Papéis em ordem alfabética, para o token ser estável entre emissões.
     */
    public TokenAcesso gerar(Usuario usuario, UUID sessaoId) {
        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(propriedades.jwt().duracao());
        List<String> papeis = usuario.getPapeis().stream().map(Papel::name).sorted().toList();
        boolean termosPendentes = aceites.temPendenciaBloqueante(usuario.getId());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propriedades.jwt().emissor())
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .id(UUID.randomUUID().toString())
                .claim("sid", sessaoId.toString())
                .claim("papeis", papeis)
                .claim("termos_pendentes", termosPendentes)
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificador.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenAcesso(valor, expiraEm, termosPendentes);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Access token emitido
     * Objetivo: Devolver o token e o instante em que expira.
     * Decisões: O front-end usa a expiração para renovar um pouco antes e a pendência de termos (KAN-92)
     *           para abrir o aceite antes de qualquer outra tela.
     */
    public record TokenAcesso(String valor, Instant expiraEm, boolean termosPendentes) {
    }
}
