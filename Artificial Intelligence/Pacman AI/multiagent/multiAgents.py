# multiAgents.py
# --------------
# Licensing Information:  You are free to use or extend these projects for
# educational purposes provided that (1) you do not distribute or publish
# solutions, (2) you retain this notice, and (3) you provide clear
# attribution to UC Berkeley, including a link to http://ai.berkeley.edu.
# 
# Attribution Information: The Pacman AI projects were developed at UC Berkeley.
# The core projects and autograders were primarily created by John DeNero
# (denero@cs.berkeley.edu) and Dan Klein (klein@cs.berkeley.edu).
# Student side autograding was added by Brad Miller, Nick Hay, and
# Pieter Abbeel (pabbeel@cs.berkeley.edu).


from util import manhattanDistance
from game import Directions
import random, util, math

from game import Agent

class ReflexAgent(Agent):
    """
    A reflex agent chooses an action at each choice point by examining
    its alternatives via a state evaluation function.

    The code below is provided as a guide.  You are welcome to change
    it in any way you see fit, so long as you don't touch our method
    headers.
    """


    def getAction(self, gameState):
        """
        You do not need to change this method, but you're welcome to.

        getAction chooses among the best options according to the evaluation function.

        Just like in the previous project, getAction takes a GameState and returns
        some Directions.X for some X in the set {NORTH, SOUTH, WEST, EAST, STOP}
        """
        # Collect legal moves and successor states
        legalMoves = gameState.getLegalActions()

        # Choose one of the best actions
        scores = [self.evaluationFunction(gameState, action) for action in legalMoves]
        bestScore = max(scores)
        bestIndices = [index for index in range(len(scores)) if scores[index] == bestScore]
        chosenIndex = random.choice(bestIndices) # Pick randomly among the best

        "Add more of your code here if you want to"

        return legalMoves[chosenIndex]

    def evaluationFunction(self, currentGameState, action):
        """
        Design a better evaluation function here.

        The evaluation function takes in the current and proposed successor
        GameStates (pacman.py) and returns a number, where higher numbers are better.

        The code below extracts some useful information from the state, like the
        remaining food (newFood) and Pacman position after moving (newPos).
        newScaredTimes holds the number of moves that each ghost will remain
        scared because of Pacman having eaten a power pellet.

        Print out these variables to see what you're getting, then combine them
        to create a masterful evaluation function.
        """
        # Useful information you can extract from a GameState (pacman.py)
        successorGameState = currentGameState.generatePacmanSuccessor(action)
        newPos = successorGameState.getPacmanPosition()
        newFood = successorGameState.getFood()
        newGhostStates = successorGameState.getGhostStates()
        newScaredTimes = [ghostState.scaredTimer for ghostState in newGhostStates]

        "*** YOUR CODE HERE ***"
        ScaredOrNot = [True if x >0 else False for x in newScaredTimes]
        newGhostPos = successorGameState.getGhostPositions()
        foodDis = [manhattanDistance(newPos,x) for x in newFood.asList()]
        minFoodDis = min(foodDis) if len(foodDis)>0 else 0.1
        GhostsDis = [manhattanDistance(newPos,x) for x in newGhostPos]
        weight = 1
        totalweights = 0
        i = 0
        for e in sorted(GhostsDis):
            if ScaredOrNot[i]:
                totalweights += 1000/(e*weight)
            else :
                totalweights += e*weight
            weight *= 0.5
            i += 1
        return 10/minFoodDis + totalweights + 2*successorGameState.getScore()

def scoreEvaluationFunction(currentGameState):
    """
    This default evaluation function just returns the score of the state.
    The score is the same one displayed in the Pacman GUI.

    This evaluation function is meant for use with adversarial search agents
    (not reflex agents).
    """
    return currentGameState.getScore()

class MultiAgentSearchAgent(Agent):
    """
    This class provides some common elements to all of your
    multi-agent searchers.  Any methods defined here will be available
    to the MinimaxPacmanAgent, AlphaBetaPacmanAgent & ExpectimaxPacmanAgent.

    You *do not* need to make any changes here, but you can if you want to
    add functionality to all your adversarial search agents.  Please do not
    remove anything, however.

    Note: this is an abstract class: one that should not be instantiated.  It's
    only partially specified, and designed to be extended.  Agent (game.py)
    is another abstract class.
    """

    def __init__(self, evalFn = 'scoreEvaluationFunction', depth = '2'):
        self.index = 0 # Pacman is always agent index 0
        self.evaluationFunction = util.lookup(evalFn, globals())
        self.depth = int(depth)

class MinimaxAgent(MultiAgentSearchAgent):
    """
    Your minimax agent (question 2)
    """

    def getAction(self, gameState):
        """
        Returns the minimax action from the current gameState using self.depth
        and self.evaluationFunction.

        Here are some method calls that might be useful when implementing minimax.

        gameState.getLegalActions(agentIndex):
        Returns a list of legal actions for an agent
        agentIndex=0 means Pacman, ghosts are >= 1

        gameState.generateSuccessor(agentIndex, action):
        Returns the successor game state after an agent takes an action

        gameState.getNumAgents():
        Returns the total number of agents in the game

        gameState.isWin():
        Returns whether or not the game state is a winning state

        gameState.isLose():
        Returns whether or not the game state is a losing state
        """
        "*** YOUR CODE HERE ***"
        return self.value(gameState,0,0)[1]

    def value(self,gameState,depth,agentIndex):
        if gameState.isWin() or gameState.isLose() or depth == self.depth:
            return (self.evaluationFunction(gameState),Directions.STOP)
        if agentIndex == 0:
            return self.maxValue(gameState,depth,0)
        else:
            return self.minValue(gameState,depth,agentIndex)

    def maxValue(self,gameState,depth,agentIndex):
        v = float('-inf')
        actions = gameState.getLegalActions(agentIndex)
        for act in actions:
            successor = gameState.generateSuccessor(agentIndex,act)
            if agentIndex +1 == gameState.getNumAgents():
                newV = self.value(successor,depth+1,0)[0]
            else:
                newV = self.value(successor,depth,agentIndex+1)[0]
            if (newV > v):
                v = newV
                action = act
        return (v,action)

    def minValue(self,gameState,depth,agentIndex):
        v = float('inf')
        actions = gameState.getLegalActions(agentIndex)
        for act in actions:
            successor = gameState.generateSuccessor(agentIndex,act)
            if agentIndex +1 == gameState.getNumAgents():
                newV = self.value(successor,depth+1,0)[0]
            else:
                newV = self.value(successor,depth,agentIndex+1)[0]
            if (newV < v):
                v = newV
                action = act
        return (v,action)


class AlphaBetaAgent(MultiAgentSearchAgent):
    """
    Your minimax agent with alpha-beta pruning (question 3)
    """

    def getAction(self, gameState):
        """
        Returns the minimax action using self.depth and self.evaluationFunction
        """
        "*** YOUR CODE HERE ***"
        return self.value(gameState,0,0,float('-inf'),float('inf'))[1]

    def value(self,gameState,depth,agentIndex,a,b):
        if gameState.isWin() or gameState.isLose() or depth == self.depth:
            return (self.evaluationFunction(gameState),Directions.STOP)
        if agentIndex == 0:
            return self.maxValue(gameState,depth,0,a,b)
        else:
            return self.minValue(gameState,depth,agentIndex,a,b)

    def maxValue(self,gameState,depth,agentIndex,a,b):
        v = float('-inf')
        actions = gameState.getLegalActions(agentIndex)
        for act in actions:
            successor = gameState.generateSuccessor(agentIndex,act)
            if agentIndex +1 == gameState.getNumAgents():
                newV = self.value(successor,depth+1,0,a,b)[0]
            else:
                newV = self.value(successor,depth,agentIndex+1,a,b)[0]
            if (newV > v):
                v = newV
                action = act
            if v > b:
                return (v,action)
            a = max(a,v)

        return (v,action)

    def minValue(self,gameState,depth,agentIndex,a,b):
        v = float('inf')
        actions = gameState.getLegalActions(agentIndex)
        for act in actions:
            successor = gameState.generateSuccessor(agentIndex,act)
            if agentIndex +1 == gameState.getNumAgents():
                newV = self.value(successor,depth+1,0,a,b)[0]
            else:
                newV = self.value(successor,depth,agentIndex+1,a,b)[0]
            if (newV < v):
                v = newV
                action = act
            if v < a:
                return (v,action)
            b = min(b,v)
        return (v,action)
        

class ExpectimaxAgent(MultiAgentSearchAgent):
    """
      Your expectimax agent (question 4)
    """

    def getAction(self, gameState):
        """
        Returns the expectimax action using self.depth and self.evaluationFunction

        All ghosts should be modeled as choosing uniformly at random from their
        legal moves.
        """
        "*** YOUR CODE HERE ***"
        return self.value(gameState,0,0)[1]

    def value(self,gameState,depth,agentIndex):
        if gameState.isWin() or gameState.isLose() or depth == self.depth:
            return (self.evaluationFunction(gameState),Directions.STOP)
        if agentIndex == 0:
            return self.maxValue(gameState,depth,0)
        else:
            return self.expValue(gameState,depth,agentIndex)

    def maxValue(self,gameState,depth,agentIndex):
        v = float('-inf')
        actions = gameState.getLegalActions(agentIndex)
        for act in actions:
            successor = gameState.generateSuccessor(agentIndex,act)
            if agentIndex +1 == gameState.getNumAgents():
                newV = self.value(successor,depth+1,0)[0]
            else:
                newV = self.value(successor,depth,agentIndex+1)[0]
            if (newV > v):
                v = newV
                action = act
        return (v,action)

    def expValue(self,gameState,depth,agentIndex):
        v = 0
        actions = gameState.getLegalActions(agentIndex)
        p = 1/len(actions)
        for act in actions:
            successor = gameState.generateSuccessor(agentIndex,act)
            if agentIndex +1 == gameState.getNumAgents():
                newV = self.value(successor,depth+1,0)[0]
            else:
                newV = self.value(successor,depth,agentIndex+1)[0]
            v += newV*p
        return (v,None)

def betterEvaluationFunction(currentGameState):
    """
    Your extreme ghost-hunting, pellet-nabbing, food-gobbling, unstoppable
    evaluation function (question 5).

    DESCRIPTION: <write something here so we know what you did>
    """
    "*** YOUR CODE HERE ***"
    curPos = currentGameState.getPacmanPosition()
    curFood = currentGameState.getFood()
    curGhostStates = currentGameState.getGhostStates()
    curScaredTimes = [ghostState.scaredTimer for ghostState in curGhostStates]
    "*** YOUR CODE HERE ***"
    ScaredOrNot = [True if x >0 else False for x in curScaredTimes]
    curGhostPos = currentGameState.getGhostPositions()
    foodDis = [manhattanDistance(curPos,x) for x in curFood.asList()]
    minFoodDis = min(foodDis) if len(foodDis)>0 else 0.1
    GhostsDis = [manhattanDistance(curPos,x) for x in curGhostPos]
    weight = 1
    totalweights = 0
    i = 0
    for e in sorted(GhostsDis):
        if ScaredOrNot[i]:
            totalweights += 1000/(e*weight)
        else :
            totalweights += e*weight
        weight *= 0.5
        i += 1
    return 10/minFoodDis + totalweights + 2*currentGameState.getScore()

# Abbreviation
better = betterEvaluationFunction