package com.replaymod.core.versions.scheduler;

import com.replaymod.core.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.ReportedException;
import net.minecraft.ReportType;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SchedulerImpl implements  Scheduler {
    private static Minecraft mc() { return Minecraft.getInstance(); }

    @Override
    public void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException {
        if (mc().isSameThread()) {
            runnable.run();
        } else {
            executor.submit(() -> {
                runnable.run();
                return null;
            }).get(30, TimeUnit.SECONDS);
        }
    }

    @Override
    public void runPostStartup(Runnable runnable) {
        runLater(new Runnable() {
            @Override
            public void run() {
                if (mc().getOverlay() != null) {
                    // delay until after resources have been loaded
                    runLater(this);
                    return;
                }
                runnable.run();
            }
        });
    }

    private boolean inRunLater = false;
    private boolean inRenderTaskQueue = false;
    public static class ReplayModExecutor extends ReentrantBlockableEventLoop<Runnable> {
        private final Thread mcThread = Thread.currentThread();

        private ReplayModExecutor(String string_1) {
            super(string_1, false);
        }

        @Override public Runnable wrapRunnable(Runnable runnable) {
            return runnable;
        }

        @Override
        protected boolean shouldRun(Runnable runnable) {
            return true;
        }

        @Override
        protected Thread getRunningThread() {
            return mcThread;
        }

        @Override
        public void runAllTasks() {
            super.runAllTasks();
        }
    }
    public final ReplayModExecutor executor = new ReplayModExecutor("Client/ReplayMod");
    private final List<Runnable> delayedTasks = new ArrayList<>();

    @Override
    public void runTasks() {
        executor.runAllTasks();
        delayedTasks.forEach(executor::schedule);
        delayedTasks.clear();
    }

    @Override
    public void runLaterWithoutLock(Runnable runnable) {
        runLater(runnable);
    }

    @Override
    public void runLater(Runnable runnable) {
        runLater(runnable, () -> runLater(runnable));
    }

    private void runLater(Runnable runnable, Runnable defer) {
        Minecraft mc = mc();
        if (mc != null && mc.isSameThread() && inRunLater) {
            delayedTasks.add(defer);
        } else {
            executor.schedule(() -> {
                inRunLater = true;
                try {
                    runnable.run();
                } catch (ReportedException e) {
                    e.printStackTrace();
                    System.err.println(e.getReport().getFriendlyReport(ReportType.CRASH));
                    mc().delayCrash(e.getReport());
                } finally {
                    inRunLater = false;
                }
            });
        }
    }
}
