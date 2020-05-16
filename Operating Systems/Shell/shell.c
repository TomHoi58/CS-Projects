#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <signal.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#include "tokenizer.h"

/* Convenience macro to silence compiler warnings about unused function parameters. */
#define unused __attribute__((unused))
#define PATH_MAX 1024

/* Whether the shell is connected to an actual terminal or not. */
bool shell_is_interactive;

/* File descriptor for the shell input */
int shell_terminal;

/* Terminal mode settings for the shell */
struct termios shell_tmodes;

/* Process group id for the shell */
pid_t shell_pgid;

int cmd_exit(struct tokens *tokens);
int cmd_help(struct tokens *tokens);
int cmd_pwd(struct tokens *tokens);
int cmd_cd(struct tokens *tokens);
void run_program(struct tokens *tokens);
void run_program_nopath(struct tokens *tokens);
void pipe_runner(char* line);
void signal_set();

/* Built-in command functions take token array (see parse.h) and return int */
typedef int cmd_fun_t(struct tokens *tokens);

/* Built-in command struct and lookup table */
typedef struct fun_desc {
  cmd_fun_t *fun;
  char *cmd;
  char *doc;
} fun_desc_t;

fun_desc_t cmd_table[] = {
  {cmd_help, "?", "show this help menu"},
  {cmd_exit, "exit", "exit the command shell"},
  {cmd_pwd, "pwd", "show current working directory"},
  {cmd_cd, "cd", "changes the current working directory to the arugument directory"},
};

/* Prints a helpful description for the given command */
int cmd_help(unused struct tokens *tokens) {
  for (unsigned int i = 0; i < sizeof(cmd_table) / sizeof(fun_desc_t); i++)
    printf("%s - %s\n", cmd_table[i].cmd, cmd_table[i].doc);
  return 1;
}

/* Exits this shell */
int cmd_exit(unused struct tokens *tokens) {
  exit(0);
}

/* Prints the current working directory to standard output */
int cmd_pwd(unused struct tokens *tokens) {
  char* cwd = malloc(sizeof(char)*PATH_MAX);
  getcwd(cwd,PATH_MAX);
  printf("%s\n",cwd);
  return 1;
}

/* Takes one argument, a directory path, and changes the current working directory to that directory */
int cmd_cd(unused struct tokens *tokens) {
  chdir(tokens_get_token(tokens,1));
  return 1;
}

void run_program(struct tokens *tokens){
  int num_args = tokens_get_length(tokens);
  char** argv = (char**)malloc(num_args*sizeof(char*)+1);
  int child_success = 0;
  bool redirect_stdin = false;
  bool redirect_stdout = false;
  char* inputfile;
  char* outputfile;

  for (int i = 0; i < num_args; ++i)
  {
    if (strcmp(tokens_get_token(tokens,i),"<") == 0){
      redirect_stdin = true;
      inputfile = tokens_get_token(tokens,i+1);
      break;
    }else if (strcmp(tokens_get_token(tokens,i),">") == 0){
      redirect_stdout = true;
      outputfile = tokens_get_token(tokens,i+1);
      break;
    }
    argv[i] = tokens_get_token(tokens,i);
  }
  argv[num_args] = NULL;

  pid_t pid = fork(); 

  

  //child execute program
  if (pid == 0){
    signal(SIGINT,SIG_DFL);
    if (redirect_stdin){
      int input = open(inputfile, O_RDONLY,0666);
      if(dup2(input, 0) < 0) {
        printf("Unable to duplicate file descriptor.");
        exit(-1);
      }
    }else if (redirect_stdout){
      int output = open(outputfile, O_WRONLY | O_CREAT,0666);
      if(dup2(output, 1) < 0) {
        printf("Unable to duplicate file descriptor.");
        exit(-1);
      }
    }
    execv(argv[0], argv);
    exit(2);
  }else {
        signal(SIGINT,SIG_IGN);
        waitpid(pid,&child_success,0);
        if (WIFEXITED(child_success)) {
          if (WEXITSTATUS(child_success) == 2){
            run_program_nopath(tokens);
          }
        }
  
    
  }
  
}

void run_program_nopath(struct tokens *tokens){
  char* path = getenv("PATH");
  char* token;
  char* save_ptr;
  bool redirect_stdin = false;
  bool redirect_stdout = false;
  char* inputfile;
  char* outputfile;

  for (token = strtok_r (path, ":", &save_ptr); token != NULL; token = strtok_r (NULL, ":", &save_ptr)) {
    int num_args = tokens_get_length(tokens);
    char** argv = (char**)malloc(num_args*sizeof(char*)+1);
    int child_success = 0;
    char* concatpath = malloc(strlen(token) + strlen(tokens_get_token(tokens,0)) + 2);
    strcpy(concatpath, token);
    strcat(concatpath, "/");
    strcat(concatpath, tokens_get_token(tokens,0));
    argv[0] = concatpath;
    // printf("%s\n",argv[0]);
    for (int i = 1; i < num_args; ++i)
    {
      if (strcmp(tokens_get_token(tokens,i),"<") == 0){
      redirect_stdin = true;
      inputfile = tokens_get_token(tokens,i+1);
      break;
    }else if (strcmp(tokens_get_token(tokens,i),">") == 0){
      redirect_stdout = true;
      outputfile = tokens_get_token(tokens,i+1);
      break;
    }
      argv[i] = tokens_get_token(tokens,i);
    }
      argv[num_args] = NULL;

    pid_t pid = fork(); 

    
    //child execute program
    if (pid == 0){
      signal(SIGINT,SIG_DFL);
      if (redirect_stdin){
        int input = open(inputfile, O_RDONLY, 0666);
        if(dup2(input, 0) < 0) {
          printf("Unable to duplicate file descriptor.");
          exit(-1);
          }
        }else if (redirect_stdout){
        int output = open(outputfile, O_WRONLY| O_CREAT,0666);
        if(dup2(output, 1 ) < 0) {
          printf("Unable to duplicate file descriptor 2.");
          exit(-1);
        }
      }
      execv(argv[0], argv);
      exit(2);
    }else {
        signal(SIGINT,SIG_IGN);
        waitpid(pid,&child_success,0);
        if (WIFEXITED(child_success)) {
          // printf("%d\n",WEXITSTATUS(child_success));
          if (WEXITSTATUS(child_success) != 2)
          {
            break;
          }
          // printf("%d\n",WEXITSTATUS(child_success));
        }
  
    
  }
    
    
  }

}

void pipe_runner(char* line){
  int num_pipes = 0;
  char* token;
  char* save_ptr;
  char** programs = malloc(sizeof(char*)*30);
  // printf("%s\n", "cool");
  for (token = strtok_r (line, "|", &save_ptr); token != NULL; token = strtok_r (NULL, "|", &save_ptr)){
    programs[num_pipes]=token;
    // printf("%s\n", programs[num_pipes]);
    num_pipes++;
  }
  num_pipes --;
  // printf("%s\n", programs[0]);
  // printf("%s\n", programs[1]);

  //set up pipes
  int** pipearray = malloc(sizeof(int*)*num_pipes);
  for (int j = 0; j < num_pipes; ++j)
  {
    int* p = malloc(sizeof(int)*2);
    pipearray[j] = p;
    pipe(p);
  }
  // printf("%s\n", "get here");

  //execute children processes
  // printf("%d\n", num_pipes);
  for (int k = 0; k <num_pipes; ++k){

    //tokenize the each program args
    struct tokens *tokens = tokenize(programs[k]); 

    // int num_args = tokens_get_length(tokens);
    // char** argv = (char**)malloc(num_args*sizeof(char*)+1);
    // for(int l = 0; l< num_args; ++l){
    //   argv[l] = tokens_get_token(tokens,l);
    // }
    // argv[num_args] = NULL;
    // printf("%s\n", argv[0]);

    pid_t pid = fork();
    if(pid == 0){
      if (k == 0){
        dup2(pipearray[k][1],1);
        run_program(tokens);
        // execv(argv[0], argv);
        // close(pipearray[k][1]);
        // close(pipearray[k][0]);
        exit(0);
      }else{
        dup2(pipearray[k-1][0],0);
        dup2(pipearray[k][1],1);
        run_program(tokens);
        // execv(argv[0], argv);
        close(pipearray[k][1]);
        // close(pipearray[k-1][0]);
        exit(0);
      }
    
    }else{
      close(pipearray[k][1]);
      wait(NULL);
    }
  }

  //for the last child
  struct tokens *tokens = tokenize(programs[num_pipes]); 

    // int num_args = tokens_get_length(tokens);
    // char** argv = (char**)malloc(num_args*sizeof(char*)+1);
    // for(int l = 0; l< num_args; ++l){
    //   argv[l] = tokens_get_token(tokens,l);
    // }
    // argv[num_args] = NULL;


    pid_t pid = fork();
    if(pid == 0){
      dup2(pipearray[num_pipes-1][0],0);
      run_program(tokens);
      // execv(argv[0], argv);
      // close(pipearray[num_pipes-1][0]);
      exit(0);
    }else{
      wait(NULL);
      // printf("%s\n", "done");
    }
          
    
}



/* Looks up the built-in command, if it exists. */
int lookup(char cmd[]) {
  for (unsigned int i = 0; i < sizeof(cmd_table) / sizeof(fun_desc_t); i++)
    if (cmd && (strcmp(cmd_table[i].cmd, cmd) == 0))
      return i;
  return -1;
}

/* Intialization procedures for this shell */
void init_shell() {
  /* Our shell is connected to standard input. */
  shell_terminal = STDIN_FILENO;

  /* Check if we are running interactively */
  shell_is_interactive = isatty(shell_terminal);

  if (shell_is_interactive) {
    /* If the shell is not currently in the foreground, we must pause the shell until it becomes a
     * foreground process. We use SIGTTIN to pause the shell. When the shell gets moved to the
     * foreground, we'll receive a SIGCONT. */
    while (tcgetpgrp(shell_terminal) != (shell_pgid = getpgrp()))
      kill(-shell_pgid, SIGTTIN);


    /* Saves the shell's process id */
    shell_pgid = getpid();

    /* Take control of the terminal */
    tcsetpgrp(shell_terminal, shell_pgid);

    /* Save the current termios to a variable, so it can be restored later. */
    tcgetattr(shell_terminal, &shell_tmodes);
  }
}

int main(unused int argc, unused char *argv[]) {
  init_shell();

  static char line[4096];
  int line_num = 0;
  bool pipe_need = false;

  /* Please only print shell prompts when standard input is not a tty */
  if (shell_is_interactive)
    fprintf(stdout, "%d: ", line_num);

  while (fgets(line, 4096, stdin)) {

    /* Check if pipe is needed */
    if (strstr(line,"|") != NULL){
      pipe_need = true;
      // printf("%s\n", line);
    }

    /* Split our line into words. */
    struct tokens *tokens = tokenize(line);


    /* Find which built-in function to run. */
    int fundex = lookup(tokens_get_token(tokens, 0));

    if (fundex >= 0) {
      cmd_table[fundex].fun(tokens);
    } else {
      /* run commands as programs. */
      if (pipe_need){
        // printf("%s\n", "pipe_need");
        pipe_runner(line);
      }else{
        run_program(tokens);
      }


    }

    if (shell_is_interactive)
      /* Please only print shell prompts when standard input is not a tty */
      fprintf(stdout, "%d: ", ++line_num);

    /* Clean up memory */
    tokens_destroy(tokens);
  }

  return 0;
}


