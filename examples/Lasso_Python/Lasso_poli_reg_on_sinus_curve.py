#source of the example: https://www.analyticsvidhya.com/blog/2016/01/complete-tutorial-ridge-lasso-regression-python/
import sys

#Importing libraries. The same will be used throughout the article.
import numpy as np
import pandas as pd
import random
import matplotlib.pyplot as plt
#%matplotlib inline
#from matplotlib.pylab import rcParams
#rcParams['figure.figsize'] = 12, 10

#Define input array with angles from 60deg to 300deg converted to radians


from sklearn.linear_model import Lasso

def lasso_regression(data, predictors, alpha, models_to_plot={}):
    #Fit the model
    lassoreg = Lasso(alpha=alpha,normalize=True, max_iter=1e5)
    lassoreg.fit(data[predictors],data['y'])
    y_pred = lassoreg.predict(data[predictors])
    
    #Check if a plot is to be made for the entered alpha
    '''if alpha in models_to_plot:
        plt.subplot(models_to_plot[alpha])
        plt.tight_layout()
        plt.plot(data['x'],y_pred)
        plt.plot(data['x'],data['y'],'.')
        plt.title('Plot for alpha: %.3g'%alpha)'''
    
    #Return the result in pre-defined format
    rss = sum((y_pred-data['y'])**2)
    #ret = [rss]
    #ret.extend([lassoreg.intercept_])
    #ret.extend(lassoreg.coef_)
    return rss

alpha_lasso = float(sys.argv[1])
data = pd.read_csv('data.csv')

#Initialize predictors to all 15 powers of x
predictors=['x']
predictors.extend(['x_%d'%i for i in range(2,16)])

#Define the alpha values to test
#alpha_lasso = [1e-15, 1e-10, 1e-8, 1e-5,1e-4, 1e-3,1e-2, 1, 5, 10]

#Initialize the dataframe to store coefficients
col = ['rss','intercept'] + ['coef_x_%d'%i for i in range(1,16)]
ind = ['alpha_%.2g'%alpha_lasso for i in range(0,10)]
coef_matrix_lasso = pd.DataFrame(index=ind, columns=col)

#Define the models to plot
#models_to_plot = {1e-10:231, 1e-5:232,1e-4:233, 1e-3:234, 1e-2:235, 1:236}

print('lasso received', alpha_lasso)
#print("rss", lasso_regression(data, predictors, alpha_lasso ''', models_to_plot''') )   
print("rss", lasso_regression(data, predictors, alpha_lasso) )   