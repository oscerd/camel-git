package org.apache.camel.component.git;

import java.io.File;
import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitProducer extends DefaultProducer{

    private static final Logger LOG = LoggerFactory.getLogger(GitProducer.class);
    private final GitEndpoint endpoint;
    
	public GitProducer(GitEndpoint endpoint) {
		super(endpoint);
		this.endpoint = endpoint;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
        String operation;	
        Repository repo;
	    if (ObjectHelper.isEmpty(endpoint.getOperation())) {
	        operation = exchange.getIn().getHeader(GitConstants.GIT_OPERATION, String.class);
	    } else {
	    	operation = endpoint.getOperation();
	    }
    	if (ObjectHelper.isEmpty(endpoint.getLocalPath())) {
    		throw new IllegalArgumentException("Local path must specified to execute " + operation);
    	}
    	repo = getLocalRepository();
	    
	    switch (operation) {
	    case GitOperation.CLONE_OPERATION:
	    	doClone(exchange, operation);
	    	break;
	    	
	    case GitOperation.INIT_OPERATION:
	    	doInit(exchange, operation);
	    	break;

	    case GitOperation.ADD_OPERATION:
	    	doAdd(exchange, operation, repo);
	    	break;
	    	
            case GitOperation.REMOVE_OPERATION:
                doRemove(exchange, operation, repo);
                break;
	    	
	    case GitOperation.COMMIT_OPERATION:
	    	doCommit(exchange, operation, repo);
	    	break;
	    
            case GitOperation.COMMIT_ALL_OPERATION:
                doCommitAll(exchange, operation, repo);
                break;
                
            case GitOperation.CREATE_BRANCH_OPERATION:
                doCreateBranch(exchange, operation, repo);
                break;
                
            case GitOperation.DELETE_BRANCH_OPERATION:
                doDeleteBranch(exchange, operation, repo);
                break;
                
            case GitOperation.STATUS_OPERATION:
                doStatus(exchange, operation, repo);
                break;
                
            case GitOperation.LOG_OPERATION:
                doLog(exchange, operation, repo);
                break;
                
            case GitOperation.PUSH_OPERATION:
                doPush(exchange, operation, repo);
                break;
                            
            case GitOperation.PULL_OPERATION:
                doPull(exchange, operation, repo);
                break;
	    }
	    repo.close();
	}
	
    protected void doClone(Exchange exchange, String operation) {
    	Git result = null;
    	if (ObjectHelper.isEmpty(endpoint.getLocalPath())) {
    		throw new IllegalArgumentException("Local path must specified to execute " + operation);
    	}
    	try {
    		File localRepo = new File(endpoint.getLocalPath(), "");
    		if (!localRepo.exists()) {
			   result = Git.cloneRepository().setURI(endpoint.getRemotePath()).setDirectory(new File(endpoint.getLocalPath(),"")).call();
    		} else {
               throw new IllegalArgumentException("The local repository directory already exists");
    		}
		} catch (Exception e) {
			LOG.error("There was an error in Git " + operation + " operation");
			e.printStackTrace();
		} finally {
			result.close();
		}
    }

    protected void doInit(Exchange exchange, String operation) {
    	Git result = null;
    	if (ObjectHelper.isEmpty(endpoint.getLocalPath())) {
    		throw new IllegalArgumentException("Local path must specified to execute " + operation);
    	}
    	try {
			result = Git.init().setDirectory(new File(endpoint.getLocalPath(),"")).setBare(false).call();
		} catch (Exception e) {
			LOG.error("There was an error in Git " + operation + " operation");
			e.printStackTrace();
		} finally {
			result.close();
		}
    }
    
    protected void doAdd(Exchange exchange, String operation, Repository repo) {
    	Git git = null;
    	String fileName = null;
    	if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(GitConstants.GIT_FILE_NAME))) {
    		fileName = exchange.getIn().getHeader(GitConstants.GIT_FILE_NAME, String.class);
    	} else {
    		throw new IllegalArgumentException("File name must be specified to execute " + operation);
    	}
    	try {
    		git = new Git(repo);
                if (ObjectHelper.isNotEmpty(endpoint.getBranchName())) {
                    git.checkout().setCreateBranch(false).setName(endpoint.getBranchName()).call();
                }
			git.add().addFilepattern(fileName).call();
		} catch (Exception e) {
			LOG.error("There was an error in Git " + operation + " operation");
			e.printStackTrace();
		}
    }
    
    protected void doRemove(Exchange exchange, String operation, Repository repo) {
        Git git = null;
        String fileName = null;
        if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(GitConstants.GIT_FILE_NAME))) {
                fileName = exchange.getIn().getHeader(GitConstants.GIT_FILE_NAME, String.class);
        } else {
                throw new IllegalArgumentException("File name must be specified to execute " + operation);
        }
        try {
                git = new Git(repo);
                if (ObjectHelper.isNotEmpty(endpoint.getBranchName())) {
                    git.checkout().setCreateBranch(false).setName(endpoint.getBranchName()).call();
                }
                        git.rm().addFilepattern(fileName).call();
                } catch (Exception e) {
                        LOG.error("There was an error in Git " + operation + " operation");
                        e.printStackTrace();
                }
    }
    
    protected void doCommit(Exchange exchange, String operation, Repository repo) {
    	Git git = null;
    	String commitMessage = null;
    	if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(GitConstants.GIT_COMMIT_MESSAGE))) {
    		commitMessage = exchange.getIn().getHeader(GitConstants.GIT_COMMIT_MESSAGE, String.class);
    	} else {
    		throw new IllegalArgumentException("Commit message must be specified to execute " + operation);
    	}
    	try {
            git = new Git(repo);
            if (ObjectHelper.isNotEmpty(endpoint.getBranchName())) {
                git.checkout().setCreateBranch(false).setName(endpoint.getBranchName()).call();
            }
    		git.commit().setMessage(commitMessage).call();
		} catch (Exception e) {
			LOG.error("There was an error in Git " + operation + " operation");
			e.printStackTrace();
		}
    }
    
    protected void doCommitAll(Exchange exchange, String operation, Repository repo) {
        Git git = null;
        String commitMessage = null;
        if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(GitConstants.GIT_COMMIT_MESSAGE))) {
                commitMessage = exchange.getIn().getHeader(GitConstants.GIT_COMMIT_MESSAGE, String.class);
        } else {
                throw new IllegalArgumentException("Commit message must be specified to execute " + operation);
        }
        try {
            git = new Git(repo);
            if (ObjectHelper.isNotEmpty(endpoint.getBranchName())) {
                git.checkout().setCreateBranch(false).setName(endpoint.getBranchName()).call();
            }
                git.commit().setAll(true).setMessage(commitMessage).call();
                } catch (Exception e) {
                        LOG.error("There was an error in Git " + operation + " operation");
                        e.printStackTrace();
                }
    }
    
    protected void doCreateBranch(Exchange exchange, String operation, Repository repo) {
        Git git = null;
        if (ObjectHelper.isEmpty(endpoint.getBranchName())) {
            throw new IllegalArgumentException("Branch Name must be specified to execute " + operation);
        } 
        try {
            git = new Git(repo);
            git.branchCreate().setName(endpoint.getBranchName()).call();
        } catch (Exception e) {
            LOG.error("There was an error in Git " + operation + " operation");
            e.printStackTrace();
        }
    }
    
    protected void doDeleteBranch(Exchange exchange, String operation, Repository repo) {
        Git git = null;
        if (ObjectHelper.isEmpty(endpoint.getBranchName())) {
            throw new IllegalArgumentException("Branch Name must be specified to execute " + operation);
        } 
        try {
            git = new Git(repo);
            git.branchDelete().setBranchNames(endpoint.getBranchName()).call();
        } catch (Exception e) {
            LOG.error("There was an error in Git " + operation + " operation");
            e.printStackTrace();
        }
    }
    
    protected void doStatus(Exchange exchange, String operation, Repository repo) {
        Git git = null;
        Status status = null;
        try {
            git = new Git(repo);
            if (ObjectHelper.isNotEmpty(endpoint.getBranchName())) {
                git.checkout().setCreateBranch(false).setName(endpoint.getBranchName()).call();
            }
                status = git.status().call();
                } catch (Exception e) {
                        LOG.error("There was an error in Git " + operation + " operation");
                        e.printStackTrace();
                }
        exchange.getOut().setBody(status);
    }
    
    protected void doLog(Exchange exchange, String operation, Repository repo) {
        Git git = null;
        Iterable<RevCommit> revCommit = null;
        try {
            git = new Git(repo);
            if (ObjectHelper.isNotEmpty(endpoint.getBranchName())) {
                git.checkout().setCreateBranch(false).setName(endpoint.getBranchName()).call();
            }
                revCommit = git.log().call();
                } catch (Exception e) {
                        LOG.error("There was an error in Git " + operation + " operation");
                        e.printStackTrace();
                }
        exchange.getOut().setBody(revCommit);
    }
    
    protected void doPush(Exchange exchange, String operation, Repository repo) {
        Git git = null;
        Iterable<PushResult> result = null;
        try {
            git = new Git(repo);
            if (ObjectHelper.isEmpty(endpoint.getRemotePath())) {
                throw new IllegalArgumentException("Remote path must be specified to execute " + operation);
            } 
            if (ObjectHelper.isNotEmpty(endpoint.getBranchName())) {
                git.checkout().setCreateBranch(false).setName(endpoint.getBranchName()).call();
            } 
            if (ObjectHelper.isNotEmpty(endpoint.getUsername()) && ObjectHelper.isNotEmpty(endpoint.getPassword())) {
                UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(endpoint.getUsername(), endpoint.getPassword());
                result = git.push().setCredentialsProvider(credentials).setRemote(endpoint.getRemotePath()).call();
            } else {
                result = git.push().setRemote(endpoint.getRemotePath()).call();
            }
                } catch (Exception e) {
                        LOG.error("There was an error in Git " + operation + " operation");
                        e.printStackTrace();
                }
        exchange.getOut().setBody(result);
    }
    
    protected void doPull(Exchange exchange, String operation, Repository repo) {
        Git git = null;
        PullResult result = null;
        try {
            git = new Git(repo);
            if (ObjectHelper.isEmpty(endpoint.getRemotePath())) {
                throw new IllegalArgumentException("Remote path must be specified to execute " + operation);
            } 
            if (ObjectHelper.isNotEmpty(endpoint.getBranchName())) {
                git.checkout().setCreateBranch(false).setName(endpoint.getBranchName()).call();
            } 
            if (ObjectHelper.isNotEmpty(endpoint.getUsername()) && ObjectHelper.isNotEmpty(endpoint.getPassword())) {
                UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(endpoint.getUsername(), endpoint.getPassword());
                result = git.pull().setCredentialsProvider(credentials).setRemote(endpoint.getRemotePath()).call();
            } else {
                result = git.pull().setRemote(endpoint.getRemotePath()).call();
            }
                } catch (Exception e) {
                        LOG.error("There was an error in Git " + operation + " operation");
                        e.printStackTrace();
                }
        exchange.getOut().setBody(result);
    }
    
    private Repository getLocalRepository(){
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = null;
		try {
			repo = builder.setGitDir(new File(endpoint.getLocalPath(), ".git"))
			        .readEnvironment() // scan environment GIT_* variables
			        .findGitDir() // scan up the file system tree
			        .build();
		} catch (IOException e) {
			LOG.error("There was an error, cannot open " + endpoint.getLocalPath() + " repository");
			e.printStackTrace();
		}
		return repo;
    }
}
