/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:10
 * Cartão Jira: KAN-80
 * Título: Data local para instante
 * Objetivo: Converter um dia escolhido no calendário (horário de Brasília) para o instante que a API espera.
 * Decisões: Brasília não tem horário de verão desde 2019, então o deslocamento fixo -03:00 basta. Para um
 *           "até" inclusivo, quem chama pede o dia seguinte, porque a API trata o fim como aberto.
 */
export function inicioDoDia(dia: string, diasAMais = 0): string {
  const [ano, mes, data] = dia.split('-').map(Number)
  const utc = Date.UTC(ano, mes - 1, data + diasAMais, 3, 0, 0)
  return new Date(utc).toISOString()
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:10
 * Cartão Jira: KAN-80
 * Título: Formato de data e hora do backoffice
 * Objetivo: Exibir instantes no horário de Brasília, em português.
 * Decisões: Data curta e hora com segundos, porque na trilha a ordem dos segundos importa.
 */
export const FORMATO_DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short', timeStyle: 'medium', timeZone: 'America/Sao_Paulo',
})
