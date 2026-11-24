package br.com.orbithub.identidade.service;

import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
}
