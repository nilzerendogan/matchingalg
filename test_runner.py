#!/usr/bin/env python3
"""
GigMatch Pro Test Runner
Compile, test, compare, and grade your solution automatically.

Usage:
    python test_runner.py              # Test all cases
    python test_runner.py --type type1 # Test only type1 cases  
    python test_runner.py --type type2 # Test only type2 cases
    python test_runner.py --verbose    # Show detailed diffs
    python test_runner.py --benchmark  # Benchmark mode: only measure times
    
"""

import os
import sys
import subprocess
import time
import argparse
import glob
import filecmp
from pathlib import Path

# Configuration
SRC_DIR = "src"
OUTPUT_DIR = "output"
# Root folder containing test case groups (Type1, Type2, ...)
TESTCASE_ROOT = "test_cases"
MAIN_CLASS = "Main"

# Colors for cross-platform output
class Colors:
    def __init__(self):
        # Enable colors on Windows if supported
        if os.name == 'nt':
            try:
                import colorama
                colorama.init()
                self.enabled = True
            except ImportError:
                self.enabled = False
        else:
            # Unix-like systems generally support ANSI colors
            self.enabled = True
    
    def __getattr__(self, name):
        colors = {
            'GREEN': '\033[0;32m',
            'RED': '\033[0;31m', 
            'YELLOW': '\033[1;33m',
            'BLUE': '\033[0;34m',
            'NC': '\033[0m'  # No Color
        }
        if self.enabled and name in colors:
            return colors[name]
        return ''

colors = Colors()

def log_info(message):
    """Print info message with blue color"""
    print(f"{colors.BLUE}{message}{colors.NC}")

def log_success(message):
    """Print success message with green color"""
    print(f"{colors.GREEN}{message}{colors.NC}")

def log_warning(message):
    """Print warning message with yellow color"""
    print(f"{colors.YELLOW}{message}{colors.NC}")

def log_error(message):
    """Print error message with red color"""
    print(f"{colors.RED}{message}{colors.NC}")

def ensure_directory(path):
    """Create directory if it doesn't exist"""
    Path(path).mkdir(parents=True, exist_ok=True)

def compile_java():
    """Compile Java sources"""
    log_info("Compiling Java sources...")
    
    if not os.path.exists(SRC_DIR):
        log_error(f"✗ Source directory '{SRC_DIR}' not found")
        return False
    
    # Find all .java files
    java_files = glob.glob(os.path.join(SRC_DIR, "*.java"))
    if not java_files:
        log_error(f"✗ No Java files found in '{SRC_DIR}'")
        return False
    
    try:
        # Compile all Java files
        cmd = ["javac"] + [os.path.basename(f) for f in java_files]
        result = subprocess.run(cmd, cwd=SRC_DIR, capture_output=True, text=True)
        
        if result.returncode != 0:
            log_error("✗ Compilation failed:")
            if result.stderr:
                print("Compilation errors:")
                print(result.stderr)
            if result.stdout:
                print(result.stdout)
            return False
        
        log_success("✓ Compilation successful")
        return True
        
    except FileNotFoundError:
        log_error("✗ javac not found. Please ensure Java SDK is installed and in PATH")
        return False
    except Exception as e:
        log_error(f"✗ Compilation error: {e}")
        return False

def _normalize_type_name(t):
    """Normalize user-provided type (case-insensitive).
    Accepts 'type1' or 'Type1' and returns the folder name 'Type1'.
    """
    if not t:
        return None
    t = t.strip()
    # If user passed 'type1' -> 'Type1'
    if t.lower().startswith('type'):
        return 'Type' + t[len('type'):].lstrip(' _-')
    return t


def get_test_files(test_type=None):
    """Get list of test input files under test_cases/*/inputs.
    If test_type is provided (e.g. 'type1' or 'Type1'), only load inputs from that folder.
    Returns full paths to input files.
    """
    if not os.path.exists(TESTCASE_ROOT):
        return []

    test_type_folder = _normalize_type_name(test_type)
    input_files = []

    # If user specified a type, look only there
    if test_type_folder:
        candidate = os.path.join(TESTCASE_ROOT, test_type_folder, 'inputs')
        if os.path.exists(candidate):
            input_files.extend(glob.glob(os.path.join(candidate, '*.txt')))
    else:
        # Discover all Type* folders
        for entry in sorted(os.listdir(TESTCASE_ROOT)):
            folder = os.path.join(TESTCASE_ROOT, entry, 'inputs')
            if os.path.isdir(folder):
                input_files.extend(glob.glob(os.path.join(folder, '*.txt')))

    return sorted(input_files)

def run_single_test(input_file, verbose=False, benchmark=False):
    """Run a single test case and return result"""
    basename = os.path.splitext(os.path.basename(input_file))[0]

    # Expected file lives in the corresponding outputs folder and follows
    # the repository naming pattern: <basename>_output.txt
    inputs_dir = os.path.dirname(input_file)
    # inputs_dir is .../test_cases/TypeX/inputs
    outputs_dir = os.path.join(os.path.dirname(inputs_dir), 'outputs')
    expected_file = os.path.join(outputs_dir, f"{basename}_output.txt")

    # Actual outputs are written to the local OUTPUT_DIR to avoid overwriting
    # expected files in the test_cases tree
    ensure_directory(OUTPUT_DIR)
    actual_file = os.path.join(OUTPUT_DIR, f"{basename}.txt")
    
    result = {
        'name': basename,
        'input_file': input_file,
        'expected_file': expected_file,
        'actual_file': actual_file,
        'status': 'unknown',
        'duration': 0,
        'error_message': ''
    }
    
    # In benchmark mode, skip expected output check
    if not benchmark and not os.path.exists(expected_file):
        result['status'] = 'skip'
        result['error_message'] = 'No expected output file'
        return result
    
    try:
        # Run the Java program
        cmd = ["java", MAIN_CLASS, f"../{input_file}", f"../{actual_file}"]
        
        start_time = time.time()
        process_result = subprocess.run(
            cmd, 
            cwd=SRC_DIR,
            capture_output=True,
            text=True,
            timeout=30  # 30 second timeout
        )
        end_time = time.time()
        
        result['duration'] = end_time - start_time
        
        # Check exit code
        if process_result.returncode != 0:
            result['status'] = 'runtime_error'
            result['error_message'] = f"Exit code: {process_result.returncode}"
            if process_result.stderr:
                result['error_message'] += f"\nStderr: {process_result.stderr.strip()}"
            return result
        
        # Check if output file was created
        if not os.path.exists(actual_file):
            result['status'] = 'no_output'
            result['error_message'] = 'No output file generated'
            return result
        
        # In benchmark mode, just mark as completed without comparison
        if benchmark:
            result['status'] = 'benchmark_complete'
        else:
            # Compare files
            # Compare files (with line ending normalization)
            with open(expected_file, 'rb') as f:
                expected_raw = f.read().replace(b'\r\n', b'\n').replace(b'\r', b'\n')

            with open(actual_file, 'rb') as f:
                actual_raw = f.read().replace(b'\r\n', b'\n').replace(b'\r', b'\n')

            if expected_raw == actual_raw:
                result['status'] = 'pass'
            else:
                result['status'] = 'wrong_output'

                if verbose:
                    try:
                        expected_text = expected_raw.decode('utf-8', errors='replace')
                        actual_text = actual_raw.decode('utf-8', errors='replace')

                        import difflib
                        diff = list(difflib.unified_diff(
                            expected_text.splitlines(keepends=True),
                            actual_text.splitlines(keepends=True),
                            fromfile='expected',
                            tofile='actual',
                            n=3
                        ))
                        result['diff'] = ''.join(diff[:20])
                    except Exception:
                        result['diff'] = "Could not generate diff"

                
                
        
        return result
        
    except subprocess.TimeoutExpired:
        result['status'] = 'timeout'
        result['error_message'] = 'Test timed out (30s limit)'
        return result
    except Exception as e:
        result['status'] = 'error'
        result['error_message'] = str(e)
        return result

def run_tests(test_type=None, verbose=False, benchmark=False):
    """Run all tests and return summary"""
    if benchmark:
        log_info("Starting benchmark execution...")
        log_warning("Benchmark mode: measuring execution times only, no output comparison")
    else:
        log_info("Starting test execution...")
    
    if test_type:
        log_warning(f"Filtering tests for type: {test_type}")
    
    # Get test files
    input_files = get_test_files(test_type)
    if not input_files:
        log_warning("⚠ No test cases found")
        return {'total': 0, 'passed': 0, 'failed': 0, 'skipped': 0, 'completed': 0}
    
    if benchmark:
        log_info(f"Benchmarking {len(input_files)} test cases")
    else:
        log_info(f"Testing {len(input_files)} test cases")
    
    if not verbose:
        print("----------------------------------------")
    else:
        print("========================================")
    
    # Ensure output directory exists
    ensure_directory(OUTPUT_DIR)
    
    # Run tests
    results = []
    passed = 0
    failed = 0
    skipped = 0
    completed = 0  # For benchmark mode
    
    for i, input_file in enumerate(input_files, 1):
        result = run_single_test(input_file, verbose, benchmark)
        results.append(result)
        
        if verbose:
            action = "Benchmarking" if benchmark else "Testing"
            print(f"{colors.BLUE}[{i}] {action}: {result['name']}{colors.NC}")
        else:
            action = "Benchmarking" if benchmark else "Testing"
            print(f"{action} {result['name']} ... ", end='', flush=True)
        
        if result['status'] == 'skip':
            if verbose:
                log_warning(f"⚠ SKIP: {result['error_message']}")
                print()
            else:
                log_warning(f"⚠ SKIP: {result['name']} ({result['error_message']})")
            skipped += 1
            
        elif result['status'] == 'benchmark_complete':
            if verbose:
                log_success(f"✓ COMPLETED (Time: {result['duration']:.3f}s)")
                print()
            else:
                log_success(f"✓ {result['duration']:.3f}s")
            completed += 1
            
        elif result['status'] == 'pass':
            if verbose:
                log_success(f"✓ RESULT: PASS (Time: {result['duration']:.1f}s)")
                print()
            else:
                log_success(f"✓ PASS ({result['duration']:.1f}s)")
            passed += 1
            
        else:  # Any failure status
            failed += 1
            if verbose:
                log_error(f"✗ RESULT: {result['status'].upper().replace('_', ' ')} (Time: {result['duration']:.1f}s)")
                if result['error_message']:
                    print(f"   {result['error_message']}")
                if result.get('diff'):
                    log_warning("Expected vs Actual diff:")
                    print(result['diff'])
                print()
            else:
                log_error(f"✗ {result['status'].upper().replace('_', ' ')} ({result['duration']:.1f}s)")
    
    # Print summary
    if verbose:
        print("========================================")
        summary_title = "Final Benchmark Summary:" if benchmark else "Final Test Summary:"
        log_info(summary_title)
    else:
        print("----------------------------------------")
        summary_title = "Benchmark Summary:" if benchmark else "Test Summary:"
        log_info(summary_title)
    
    total = len(input_files)
    print(f"  Total:  {total}")
    
    if benchmark:
        print(f"  {colors.GREEN}Completed: {completed}{colors.NC}")
        print(f"  {colors.RED}Failed: {failed}{colors.NC}")
        if skipped > 0:
            print(f"  {colors.YELLOW}Skipped: {skipped}{colors.NC}")
        
        # Calculate stats for benchmark mode
        if completed > 0:
            times = [r['duration'] for r in results if r['status'] == 'benchmark_complete']
            if times:
                avg_time = sum(times) / len(times)
                min_time = min(times)
                max_time = max(times)
                print(f"  Average time: {avg_time:.3f}s")
                print(f"  Min time:     {min_time:.3f}s") 
                print(f"  Max time:     {max_time:.3f}s")
        
        if failed == 0 and completed > 0:
            log_success("🎉 All benchmarks completed!")
        elif total == 0:
            log_warning("⚠ No test cases found")
        elif failed > 0:
            log_error("❌ Some benchmarks failed")
    else:
        print(f"  {colors.GREEN}Passed: {passed}{colors.NC}")
        print(f"  {colors.RED}Failed: {failed}{colors.NC}")
        if skipped > 0:
            print(f"  {colors.YELLOW}Skipped: {skipped}{colors.NC}")
        
        if failed == 0 and total > 0:
            if verbose:
                log_success("🎉 ALL TESTS PASSED!")
            else:
                log_success("🎉 All tests passed!")
        elif total == 0:
            log_warning("⚠ No test cases found")
        else:
            if verbose:
                log_error("❌ SOME TESTS FAILED")
            else:
                log_error("❌ Some tests failed")
    
    return {
        'total': total,
        'passed': passed, 
        'failed': failed,
        'skipped': skipped,
        'completed': completed,
        'results': results
    }

def clean_outputs():
    """Clean generated output files automatically"""
    import glob
    
    # Remove old .class files
    for f in glob.glob(os.path.join(SRC_DIR, "*.class")):
        try:
            os.remove(f)
        except:
            pass
    
    # Remove old output files
    ensure_directory(OUTPUT_DIR)
    for f in glob.glob(os.path.join(OUTPUT_DIR, "*.txt")):
        try:
            os.remove(f)
        except:
            pass

def main():
    parser = argparse.ArgumentParser(description="GigMatch Pro Test Runner")
    parser.add_argument('--type', help='Filter tests by type (e.g. type1 or Type1). Case-insensitive. If omitted, runs all types')
    parser.add_argument('--verbose', '-v', action='store_true', help='Show detailed output and diffs')
    parser.add_argument('--benchmark', '-b', action='store_true', help='Benchmark mode: only measure execution times, no output comparison')
    
    args = parser.parse_args()
    
    # Change to script directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)
    
    log_info("GigMatch Pro Test Runner")
    print("=" * 40)
    
    # Always clean before testing
    clean_outputs()
    
    # Compile Java sources
    if not compile_java():
        sys.exit(1)
    
    # Run tests and grade
    summary = run_tests(args.type, args.verbose, args.benchmark)
    
    # Exit with appropriate code
    if args.benchmark:
        # In benchmark mode, success if any tests completed
        sys.exit(0 if summary['completed'] > 0 or summary['total'] == 0 else 1)
    else:
        # In normal mode, success if no failures and at least one test
        sys.exit(0 if summary['failed'] == 0 and summary['total'] > 0 else 1)

if __name__ == '__main__':
    main()