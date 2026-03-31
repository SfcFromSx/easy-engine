package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkStrategy;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobControllerTest {

    // Covers JobController#update editable field propagation.
    @Test
    void shouldUpdateJobFields() {
        BenchmarkJobRepository repository = mock(BenchmarkJobRepository.class);
        BenchmarkRunRepository runRepository = mock(BenchmarkRunRepository.class);
        JobController controller = new JobController(repository, runRepository);

        BenchmarkJob existing = new BenchmarkJob();
        existing.setId(5L);
        BenchmarkJob payload = new BenchmarkJob();
        payload.setName("updated");
        payload.setDataSourceId(9L);
        payload.setConcurrentThreads(8);
        payload.setRounds(32);
        payload.setStrategy(BenchmarkStrategy.CACHE_PENETRATION);
        payload.setTestSetId(12L);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        BenchmarkJob saved = controller.update(5L, payload);

        assertSame(existing, saved);
        assertEquals("updated", existing.getName());
        assertEquals(Long.valueOf(9L), existing.getDataSourceId());
        assertEquals(8, existing.getConcurrentThreads());
        assertEquals(32, existing.getRounds());
        assertEquals(BenchmarkStrategy.CACHE_PENETRATION, existing.getStrategy());
        assertEquals(Long.valueOf(12L), existing.getTestSetId());
    }

    // Covers JobController#remove run-history guard.
    @Test
    void shouldRejectJobRemovalWhenRunsExist() {
        BenchmarkJobRepository repository = mock(BenchmarkJobRepository.class);
        BenchmarkRunRepository runRepository = mock(BenchmarkRunRepository.class);
        JobController controller = new JobController(repository, runRepository);
        when(runRepository.existsByJobId(6L)).thenReturn(true);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> controller.remove(6L));

        assertEquals("任务已有关联运行记录，暂不支持直接删除。请先保留历史记录或手动清理相关 runs。", error.getMessage());
    }
}
