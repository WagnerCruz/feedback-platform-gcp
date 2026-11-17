package br.com.feedback.common;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Acesso à tabela avaliacao. Todas as consultas são parametrizadas (sem concatenação de SQL). */
public class AvaliacaoRepository {

    /** Fuso usado para agrupar "avaliações por dia" no relatório. */
    public static final String FUSO = "America/Sao_Paulo";

    private final DataSource ds;

    public AvaliacaoRepository(DataSource ds) {
        this.ds = ds;
    }

    public Avaliacao inserir(String descricao, int nota) {
        Urgencia urgencia = Urgencia.daNota(nota);
        String sql = "INSERT INTO avaliacao (descricao, nota, urgencia) VALUES (?, ?, ?) RETURNING id, data_envio";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, descricao);
            ps.setInt(2, nota);
            ps.setString(3, urgencia.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Avaliacao(rs.getLong("id"), descricao, nota, urgencia,
                        rs.getObject("data_envio", OffsetDateTime.class));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao inserir avaliação", e);
        }
    }

    public List<Avaliacao> listarDesde(OffsetDateTime desde) {
        String sql = "SELECT id, descricao, nota, urgencia, data_envio FROM avaliacao "
                + "WHERE data_envio >= ? ORDER BY data_envio";
        List<Avaliacao> lista = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, desde);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Avaliacao(rs.getLong("id"), rs.getString("descricao"), rs.getInt("nota"),
                            Urgencia.valueOf(rs.getString("urgencia")), rs.getObject("data_envio", OffsetDateTime.class)));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar avaliações", e);
        }
    }

    /** Média das notas desde a data informada (0 se não houver avaliações). */
    public double mediaDesde(OffsetDateTime desde) {
        String sql = "SELECT COALESCE(AVG(nota), 0) FROM avaliacao WHERE data_envio >= ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, desde);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao calcular média", e);
        }
    }

    /** Quantidade de avaliações por dia (dia no fuso de São Paulo), ordenado por data. */
    public Map<LocalDate, Long> contagemPorDiaDesde(OffsetDateTime desde) {
        String sql = "SELECT (data_envio AT TIME ZONE '" + FUSO + "')::date AS dia, COUNT(*) "
                + "FROM avaliacao WHERE data_envio >= ? GROUP BY dia ORDER BY dia";
        Map<LocalDate, Long> mapa = new TreeMap<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, desde);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date dia = rs.getDate(1);
                    mapa.put(dia.toLocalDate(), rs.getLong(2));
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao contar avaliações por dia", e);
        }
    }

    /** Quantidade de avaliações por urgência (todas as urgências aparecem, mesmo com zero). */
    public Map<Urgencia, Long> contagemPorUrgenciaDesde(OffsetDateTime desde) {
        Map<Urgencia, Long> mapa = new EnumMap<>(Urgencia.class);
        for (Urgencia u : Urgencia.values()) {
            mapa.put(u, 0L);
        }
        String sql = "SELECT urgencia, COUNT(*) FROM avaliacao WHERE data_envio >= ? GROUP BY urgencia";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, desde);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(Urgencia.valueOf(rs.getString(1)), rs.getLong(2));
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao contar avaliações por urgência", e);
        }
    }
}
