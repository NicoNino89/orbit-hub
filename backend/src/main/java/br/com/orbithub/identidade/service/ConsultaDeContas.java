package br.com.orbithub.identidade.service;

import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Consulta de contas para outros módulos
 * Objetivo: Dar aos outros módulos (auditoria, aprovações, notificações) o nome de uma conta sem que eles
 *           acessem o repositório de contas diretamente.
 * Decisões: Regra da arquitetura: um módulo só usa outro por serviços públicos. Só leitura e só o nome, o mínimo
 *           que esses módulos precisam.
 */
@Service
public class ConsultaDeContas {

    private final UsuarioRepository usuarios;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Criação do serviço
     * Objetivo: Receber o repositório de contas.
     * Decisões: Injeção por construtor.
     */
    public ConsultaDeContas(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Nome de uma conta
     * Objetivo: Devolver o nome de exibição de uma conta.
     * Decisões: Vazio quando a conta não existe.
     */
    @Transactional(readOnly = true)
    public Optional<String> nome(UUID contaId) {
        return contaId == null ? Optional.empty() : usuarios.findById(contaId).map(Usuario::getNome);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Nomes de várias contas
     * Objetivo: Devolver os nomes de um conjunto de contas numa só consulta.
     * Decisões: Contas inexistentes ficam fora do mapa.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> nomes(Collection<UUID> contas) {
        return usuarios.findAllById(contas.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNome));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: E-mail de uma conta ativa
     * Objetivo: Devolver o e-mail para entregar um aviso.
     * Decisões: Vazio se a conta não existe ou não está ativa (conta desativada não recebe e-mail).
     */
    @Transactional(readOnly = true)
    public Optional<String> emailParaAviso(UUID contaId) {
        return usuarios.findById(contaId).filter(Usuario::podeTerSessao).map(Usuario::getEmail);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Equipe ativa com papéis
     * Objetivo: Listar os membros ativos que têm algum dos papéis (por exemplo, quem aprova uma ação grave).
     * Decisões: Conjunto vazio devolve lista vazia.
     */
    @Transactional(readOnly = true)
    public List<UUID> equipeAtivaComPapeis(Set<Papel> papeis) {
        return papeis.isEmpty() ? List.of() : usuarios.idsDaEquipeAtivaComPapeis(papeis);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Contas públicas ativas
     * Objetivo: Listar as contas de investidor e empreendedor ativas.
     * Decisões: Para avisos a todos os usuários (termos atualizados).
     */
    @Transactional(readOnly = true)
    public List<UUID> contasPublicasAtivas() {
        return usuarios.idsPublicosAtivos();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Conta ativa pelo e-mail
     * Objetivo: Encontrar a conta ativa de um e-mail (por exemplo, para avisar no sino quem recebeu um convite).
     * Decisões: E-mail comparado em minúsculas; conta desativada ou pendente não é encontrada.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> contaAtivaDoEmail(String email) {
        return usuarios.findByEmail(email.strip().toLowerCase(Locale.ROOT)).filter(Usuario::podeTerSessao)
                .map(Usuario::getId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Conta é investidora
     * Objetivo: Dizer se a conta ativa tem o papel de investidor (quem pode entrar numa organização investidora).
     * Decisões: Conta inexistente ou inativa responde falso.
     */
    @Transactional(readOnly = true)
    public boolean investidoraAtiva(UUID contaId) {
        return usuarios.findById(contaId).filter(Usuario::podeTerSessao)
                .map(u -> u.getPapeis().contains(Papel.INVESTIDOR)).orElse(false);
    }
}
