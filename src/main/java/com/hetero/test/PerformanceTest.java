package com.hetero.test;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import com.hetero.model.Priority;
import com.hetero.model.Task;
import com.hetero.repository.ArrayListTaskRepo;
import com.hetero.repository.HashMapTaskRepo;
import com.hetero.repository.LinkedListTaskRepo;
import com.hetero.repository.TaskRepository;

public class PerformanceTest {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // ==================================================
        // 1. JVM WARMUP PHASE
        // ==================================================
        System.out.println("==================================================");
        System.out.println(" WARMING UP JVM & OPTIMIZING ALL 9 FEATURES...   ");
        System.out.println("==================================================");
        
        TaskRepository[] warmupRepos = {
            new ArrayListTaskRepo(), 
            new LinkedListTaskRepo(), 
            new HashMapTaskRepo()
        };
        
        for (TaskRepository repo : warmupRepos) {
            if (repo instanceof ArrayListTaskRepo) {
                ((ArrayListTaskRepo) repo).setTestMode(true);
            } else if (repo instanceof LinkedListTaskRepo) {
                ((LinkedListTaskRepo) repo).setTestMode(true);
            } else if (repo instanceof HashMapTaskRepo) {
                ((HashMapTaskRepo) repo).setTestMode(true);
            }
            
            for (int i = 0; i < 500; i++) {
                Priority p = (i % 3 == 0) ? Priority.HIGH : (i % 3 == 1) ? Priority.MEDIUM : Priority.LOW;
                String cat = (i % 2 == 0) ? "Uni" : "Work";
                LocalDate date = (i % 4 == 0) ? LocalDate.now() : LocalDate.now().plusDays(1);
                Task t = new Task("Warmup " + i, "Notes", p, cat, date);
                t.setCompleted(i % 5 == 0);
                repo.add(t);
            }
            List<Task> loaded = repo.findAll();
            if (!loaded.isEmpty()) {
                int targetId = loaded.get(loaded.size() / 2).getId();
                repo.findById(targetId);
                Task t = loaded.get(loaded.size() / 2);
                t.setNotes("Warmed");
                repo.update(t);
                repo.findByCompleted(true);
                repo.findDueToday();
                repo.findByCategory("Uni");
                repo.findByPriority(Priority.HIGH);
                repo.delete(targetId);
            }
        }
        System.out.println(" Warmup complete! JIT compiler hotspots optimized.\n");

        // ==================================================
        // 2. INTERACTIVE MENU LOOP
        // ==================================================
        while (true) {
            System.out.println("==================================================");
            System.out.println("     COMPLEXITY ANALYSER & BENCHMARK TOOL        ");
            System.out.println("==================================================");
            System.out.println("Select a Data Structure to evaluate:");
            System.out.println("1. ArrayList (ArrayListTaskRepo)");
            System.out.println("2. LinkedList (LinkedListTaskRepo)");
            System.out.println("3. HashMap (HashMapTaskRepo)");
            System.out.println("4. Run All Structures (Side-by-Side Comparison)");
            System.out.println("5. Exit Program");
            System.out.print("Enter your choice (1-5): ");

            int choice = 0;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("\n Invalid choice! Enter a number from 1 to 5.\n");
                continue;
            }

            if (choice == 5) {
                System.out.println("\nExiting metrics tool. Analysis complete.");
                break;
            }

            if (choice < 1 || choice > 5) {
                System.out.println("\n Choice out of bounds!\n");
                continue;
            }

            System.out.print("Enter the number of requests/tasks to simulate: ");
            int size = 0;
            try {
                size = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("\n Invalid scale size! Returning to menu.\n");
                continue;
            }

            if (size <= 0) {
                System.out.println("\n Simulation size must be greater than 0.\n");
                continue;
            }

            System.out.println("\nRunning resource usage tracking profile for size: " + size + "...\n");

            switch (choice) {
                case 1:
                    runFullSuite(new ArrayListTaskRepo(), size);
                    break;
                case 2:
                    runFullSuite(new LinkedListTaskRepo(), size);
                    break;
                case 3:
                    runFullSuite(new HashMapTaskRepo(), size);
                    break;
                case 4:
                    System.out.println("============= START COMPARATIVE RUN =============");
                    runFullSuite(new ArrayListTaskRepo(), size);
                    System.out.println("--------------------------------------------------");
                    runFullSuite(new LinkedListTaskRepo(), size);
                    System.out.println("--------------------------------------------------");
                    runFullSuite(new HashMapTaskRepo(), size);
                    System.out.println("============== END COMPARATIVE RUN ==============");
                    break;
            }

            System.out.print("\nPress Enter to return to the evaluation menu...");
            scanner.nextLine();
        }

        scanner.close();
    }

    // ==================================================
    // 3. METRICS EVALUATION ENGINE (TIME & SPACE)
    // ==================================================
    private static void runFullSuite(TaskRepository repo, int size) {
        if (repo instanceof ArrayListTaskRepo) {
            ((ArrayListTaskRepo) repo).setTestMode(true);
        } else if (repo instanceof LinkedListTaskRepo) {
            ((LinkedListTaskRepo) repo).setTestMode(true);
        } else if (repo instanceof HashMapTaskRepo) {
            ((HashMapTaskRepo) repo).setTestMode(true);
        }

        String repoName = repo.getClass().getSimpleName();
        System.out.println("--------------------------------------------------");
        System.out.println(" STRUCTURE PROFILE: " + repoName.toUpperCase());
        System.out.println("--------------------------------------------------");

        Runtime runtime = Runtime.getRuntime();
        
        // 1. FEATURE: Add/Insert Task (Bulk)
        System.gc(); 
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long start = System.nanoTime();
        
        for (int i = 0; i < size; i++) {
            Priority priority = (i % 3 == 0) ? Priority.HIGH : (i % 3 == 1) ? Priority.MEDIUM : Priority.LOW;
            String category = (i % 2 == 0) ? "Uni" : "Work";
            LocalDate dueDate = (i % 4 == 0) ? LocalDate.now() : LocalDate.now().plusDays(1);
            
            Task task = new Task("Task " + i, "Description " + i, priority, category, dueDate);
            task.setCompleted(i % 5 == 0); 
            repo.add(task);
        }
        long insertTime = System.nanoTime() - start;
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long insertSpace = Math.max(0, memAfter - memBefore);

        List<Task> allTasks = repo.findAll();
        if (allTasks.isEmpty()) {
            System.out.println("  [Error] Dataset allocation mismatch.");
            return;
        }

        Task targetTask = allTasks.get(size / 2);
        int targetId = targetTask.getId();

        // 2. FEATURE: List All Tasks
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        start = System.nanoTime();
        repo.findAll();
        long listTime = System.nanoTime() - start;
        memAfter = runtime.totalMemory() - runtime.freeMemory();
        long listSpace = Math.max(0, memAfter - memBefore);

        // 3. FEATURE: Update Task Details
        targetTask.setNotes("Modified Content Tracking");
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        start = System.nanoTime();
        repo.update(targetTask);
        long updateTime = System.nanoTime() - start;
        memAfter = runtime.totalMemory() - runtime.freeMemory();
        long updateSpace = Math.max(0, memAfter - memBefore);

        // 4. FEATURE: Find Task by ID
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        start = System.nanoTime();
        repo.findById(targetId);
        long findIdTime = System.nanoTime() - start;
        memAfter = runtime.totalMemory() - runtime.freeMemory();
        long findIdSpace = Math.max(0, memAfter - memBefore);

        // 5. FEATURE: Filter by Completion Status
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        start = System.nanoTime();
        repo.findByCompleted(true);
        long filterCompTime = System.nanoTime() - start;
        memAfter = runtime.totalMemory() - runtime.freeMemory();
        long filterCompSpace = Math.max(0, memAfter - memBefore);

        // 6. FEATURE: Filter by Tasks Due Today
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        start = System.nanoTime();
        repo.findDueToday();
        long filterTodayTime = System.nanoTime() - start;
        memAfter = runtime.totalMemory() - runtime.freeMemory();
        long filterTodaySpace = Math.max(0, memAfter - memBefore);

        // 7. FEATURE: Filter by Category
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        start = System.nanoTime();
        repo.findByCategory("Uni");
        long filterCatTime = System.nanoTime() - start;
        memAfter = runtime.totalMemory() - runtime.freeMemory();
        long filterCatSpace = Math.max(0, memAfter - memBefore);

        // 8. FEATURE: Filter by Priority
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        start = System.nanoTime();
        repo.findByPriority(Priority.HIGH);
        long filterPriorTime = System.nanoTime() - start;
        memAfter = runtime.totalMemory() - runtime.freeMemory();
        long filterPriorSpace = Math.max(0, memAfter - memBefore);

        // 9. FEATURE: Delete Task
        System.gc();
        memBefore = runtime.totalMemory() - runtime.freeMemory();
        start = System.nanoTime();
        repo.delete(targetId);
        long deleteTime = System.nanoTime() - start;
        memAfter = runtime.totalMemory() - runtime.freeMemory();
        long deleteSpace = Math.max(0, memAfter - memBefore);

        // Output Display Matrix
        System.out.printf("%-32s | %-16s | %-16s\n", "Feature Operation Evaluated", "Execution (Time)", "Heap Space (Delta)");
        System.out.println("---------------------------------------------------------------------------------");
        System.out.printf("1. Add/Insert Task (Bulk Setup)  | %11.4f ms | %11.2f KB\n", insertTime / 1_000_000.0, insertSpace / 1024.0);
        System.out.printf("2. List All Tasks (findAll)      | %11.4f ms | %11.2f KB\n", listTime / 1_000_000.0, listSpace / 1024.0);
        System.out.printf("3. Update Task Details (update)  | %11.4f ms | %11.2f KB\n", updateTime / 1_000_000.0, updateSpace / 1024.0);
        System.out.printf("4. Find Task by ID (findById)     | %11.4f ms | %11.2f KB\n", findIdTime / 1_000_000.0, findIdSpace / 1024.0);
        System.out.printf("5. Filter by Completion Status    | %11.4f ms | %11.2f KB\n", filterCompTime / 1_000_000.0, filterCompSpace / 1024.0);
        System.out.printf("6. Filter by Tasks Due Today     | %11.4f ms | %11.2f KB\n", filterTodayTime / 1_000_000.0, filterTodaySpace / 1024.0);
        System.out.printf("7. Filter by Category             | %11.4f ms | %11.2f KB\n", filterCatTime / 1_000_000.0, filterCatSpace / 1024.0);
        System.out.printf("8. Filter by Priority             | %11.4f ms | %11.2f KB\n", filterPriorTime / 1_000_000.0, filterPriorSpace / 1024.0);
        System.out.printf("9. Delete Task (delete)           | %11.4f ms | %11.2f KB\n", deleteTime / 1_000_000.0, deleteSpace / 1024.0);
    }
}