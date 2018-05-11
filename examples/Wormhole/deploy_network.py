import numpy as np
import math
import networkx as nx
import scipy.spatial


''' 
generates a network into a square shaped area,
using random placement, and unit disk graph comm. model

params:
npoint: number of sensors
radius: communication radius of the sensors
side_len: length of the sides of the square
'''
def generateSquare(npoints, radius, side_len):
	# try 1000000 times
	for X in range(0,100000):	
		# make graph, add nodes and random positions
		G = nx.Graph()
		G.add_nodes_from(range(0,npoints))
		positions = {}
		positions_ = []
		for i in range(0, npoints):
			positions[i] = [np.random.rand()*side_len, np.random.rand()*side_len]
			positions_.append(positions[i])
		
		# calculate pairwise distances
		Y = scipy.spatial.distance.pdist(np.asarray(positions_), 'euclidean')
		distMatrix = scipy.spatial.distance.squareform(Y)
		
		# add edges based on radius
		for i in range(0,npoints-1):
			for j in range(i,npoints):
				if distMatrix[i,j] <= radius and i!=j:
					G.add_edge(i, j)
		# if the generated network is not connected, generate a new one
		if nx.is_connected(G):
			return positions, G
			
	print('Error: the graph is not connected with these parameters!\n')

''' 
generates a network into a square shaped area,
using random placement, and quasi unit disk graph comm. model:

params:
npoint: number of sensors
radius: communication radius of the sensors
side_len: length of the sides of the square
p: parameter for the quasi comm. model
'''
def generateSquareQuasi(npoints, radius, side_len, p=0.5):
	
	radius2 = radius
	radius1 = radius*p 
	# try 1000000 times
	for X in range(0,100000):
		# make graph, add nodes and random positions
		G = nx.Graph()
		G.add_nodes_from(range(0,npoints))
		positions = {}
		positions_ = []
		for i in range(0, npoints):
			positions[i] = [np.random.rand()*side_len, np.random.rand()*side_len]
			positions_.append(positions[i])
		
		# calculate pairwise distances
		Y = scipy.spatial.distance.pdist(np.asarray(positions_), 'euclidean')
		distMatrix = scipy.spatial.distance.squareform(Y)
		
			
		# add edges based on radius and param p
		for i in range(0,npoints-1):
			for j in range(i,npoints):
				if distMatrix[i,j] <= radius1 and i!=j:
					G.add_edge(i, j)
				if distMatrix[i,j] > radius1 and distMatrix[i,j] <=radius2:
					asd = np.random.rand()
					alpha = radius1 / radius2
					prob = (alpha/(1 - alpha)) * ((radius2/distMatrix[i,j]) - 1)
					
					if asd < prob:						
						G.add_edge(i, j)
					
		# if the generated network is not connected, generate a new one
		if nx.is_connected(G):
			return positions, G
			
	print('Error: the graph is not connected with these parameters!\n')



''' 
generates a network into a square shaped area,
using perturbed grid placement, and unit disk graph comm. model

params:
npoint: number of sensors. in this case a square number would be ideal
radius: communication radius of the sensors
side_len: length of the sides of the square
noise: noise used for perturbation
'''
def generateGrid(npoints, radius, side_len, noise=0.75):
	
	for X in range(0,10000):	
		H = int(math.sqrt(npoints))
		node_dist = side_len / H
		max_noise = node_dist*noise

		G = nx.Graph()
		G.add_nodes_from(range(0,npoints))
		positions_ = []
		positions = {}

		# deploy network on grid
		for i in range(0,H):
			for j in range(0,H):
				rx = max_noise*np.random.rand() - max_noise
				ry = max_noise*np.random.rand() - max_noise
				positions[i + j*H] = [i* node_dist + rx, j* node_dist + ry]
				positions_.append(positions[i + j*H])
				
		Y = scipy.spatial.distance.pdist(np.asarray(positions_), 'euclidean')
		distMatrix = scipy.spatial.distance.squareform(Y)

		# add edges based on radius
		for i in range(0,npoints-1):
			for j in range(i,npoints):
				if distMatrix[i,j] <= radius and i!=j:
					G.add_edge(i, j)

		if nx.is_connected(G):
			return positions, G
			
	print('Error: the graph is not connected with these parameters!\n')

		
''' 
generates a network into a square shaped area,
using perturbed grid placement, and quasi unit disk graph comm. model

params:
npoint: number of sensors. in this case a square number would be ideal
radius: communication radius of the sensors
side_len: length of the sides of the square
p: parameter for the quasi comm. model
noise: noise used for perturbation
'''	
def generateGridQuasi(npoints, radius, side_len, p=0.5, noise=0.75):
	
	radius2 = radius
	radius1 = radius*p 
	
	for i in range(0,10000):	
		H = int(math.sqrt(npoints))
		node_dist = side_len / H
		max_noise = node_dist*noise

		G = nx.Graph()
		G.add_nodes_from(range(0,npoints))
		positions_ = []
		positions = {}

		# deploy network on grid
		for i in range(0,H):
			for j in range(0,H):
				rx = max_noise*np.random.rand() - max_noise
				ry = max_noise*np.random.rand() - max_noise
				positions[i + j*H] = [i* node_dist + rx, j* node_dist + ry]
				positions_.append(positions[i + j*H])
				
		Y = scipy.spatial.distance.pdist(np.asarray(positions_), 'euclidean')
		distMatrix = scipy.spatial.distance.squareform(Y)

		# add edges based on radius
		for i in range(0,npoints-1):
			for j in range(i,npoints):
				if distMatrix[i,j] <= radius1 and i!=j:
					G.add_edge(i, j)
				if distMatrix[i,j] > radius1 and distMatrix[i,j] <=radius2:
					asd = np.random.rand()
					alpha = radius1 / radius2
					prob = (alpha/(1 - alpha)) * ((radius2/distMatrix[i,j]) - 1)
					if asd < prob:						
						G.add_edge(i, j)

		if nx.is_connected(G):
			return positions, G
			
	print('Error: the graph is not connected with these parameters!\n')
		


'''
adds a wormhole to an input network

params:
positions: positions of the sensors in the network
G: connectivity graph
WH_radius: radius of the wormhole endpoints
WH_type:

1.:  x------------x  :: only the endpoints are affected by the wormhole edges

2.:  ___
    / x \--------
   |  x  |---------x  :: on one side only the endpoint is affected, on the other side, both the enpoint and the nodes in the wormhole radius are affected
    \  x/--------

3.:  ___           ___
    /  x\---------/  x\
   |  x  |-------|  x  | :: on both sides of the wormhole, the surrounding nodes are affected, based on the wormhole radius
	\  x/---------\ x /

minDistance: the minimum hope distance of the wormhole endpoints
'''

def addWormhole(positions, G, WH_radius, WH_type, minDistance):
	npoints = len(positions)
	
	moddedG = G.copy()
	
	# randomly choose a point as an endpoint
	endpoint1 = np.random.randint(0, npoints)
	
	# choose a 2nd endpoints thats far enough
	distances = nx.shortest_path_length(G, source=endpoint1)
	possible_endpoint2s = [key for key in distances if distances[key] > minDistance]
	endpoint2 = possible_endpoint2s[np.random.randint(0, len(possible_endpoint2s))]
	
	# calculate distances
	Y = scipy.spatial.distance.pdist(np.asarray(list(positions.values())), 'euclidean')
	distMatrix = scipy.spatial.distance.squareform(Y)
	
	endpoints1 = [endpoint1]
	endpoints2 = [endpoint2]
	
	# find the other affected nodes, based on the WH_radius and WH_type	
	if WH_type >= 2:
		for i in range(0,npoints):
			if i != endpoint2 and distMatrix[endpoint2, i] < WH_radius:
				endpoints2.append(i)
	
	if WH_type == 3:
		for i in range(0,npoints):
			if i != endpoint1 and distMatrix[endpoint1, i] < WH_radius:
				endpoints1.append(i)
	
	# introduce the new wormhole edges
	for WH_point1 in endpoints1:
		for WH_point2 in endpoints2:
			moddedG.add_edge(WH_point1, WH_point2)		
	
	return moddedG, endpoints1, endpoints2
